/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jspiel.vanilla;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jspiel.api.RiffChunkDataWriterType;
import com.io7m.jspiel.api.RiffChunkID;
import com.io7m.jspiel.api.RiffFileWriterChunkDescriptionType;
import com.io7m.jspiel.api.RiffFileWriterDescriptionType;
import com.io7m.jspiel.api.RiffFileWriterProviderType;
import com.io7m.jspiel.api.RiffFileWriterType;
import com.io7m.jspiel.api.RiffOutOfBoundsException;
import com.io7m.jspiel.api.RiffSize;
import com.io7m.jspiel.api.RiffSizes;
import com.io7m.jspiel.api.RiffWriteException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * The default implementation of the {@link RiffFileWriterProviderType} interface.
 */

@Component
public final class RiffWriters implements RiffFileWriterProviderType
{
  private static final Logger LOG = LoggerFactory.getLogger(RiffWriters.class);

  private static final long CHUNK_ID_OCTETS = 4L;
  private static final long DATA_SIZE_OCTETS = 4L;
  private static final long FORM_OCTETS = 4L;
  private static final long HEADER_SIZE = CHUNK_ID_OCTETS + DATA_SIZE_OCTETS;

  /**
   * Construct a writer provider.
   */

  public RiffWriters()
  {

  }

  @Override
  public RiffFileWriterType createForChannel(
    final URI source,
    final RiffFileWriterDescriptionType description,
    final SeekableByteChannel channel)
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(channel, "channel");
    return new Writer(source, description, channel);
  }

  private static final class Writer implements RiffFileWriterType
  {
    private final URI source;
    private final RiffFileWriterDescriptionType description;
    private final SeekableByteChannel root_channel;
    private final HashMap<Long, RiffSize> sizes_written;
    private final HashMap<Long, Long> sizes_offsets;

    Writer(
      final URI in_source,
      final RiffFileWriterDescriptionType in_description,
      final SeekableByteChannel in_channel)
    {
      this.source =
        Objects.requireNonNull(in_source, "source");
      this.description =
        Objects.requireNonNull(in_description, "description");
      this.root_channel =
        Objects.requireNonNull(in_channel, "channel");

      this.sizes_written = new HashMap<>();
      this.sizes_offsets = new HashMap<>();
    }

    private static long position(final SeekableByteChannel channel)
    {
      try {
        return channel.position();
      } catch (final IOException e) {
        return -1L;
      }
    }

    private static int writeChecked(
      final SeekableByteChannel channel,
      final ByteBuffer output,
      final int expected)
      throws IOException
    {
      final var wrote = channel.write(output);
      if (wrote != expected) {
        throw new IOException(
          new StringBuilder(64)
            .append("Short write.")
            .append(System.lineSeparator())
            .append("  Offset:   0x")
            .append(Long.toUnsignedString(channel.position(), 16))
            .append(System.lineSeparator())
            .append("  Expected: Write ")
            .append(expected)
            .append(" octets")
            .append(System.lineSeparator())
            .append("  Received: Wrote ")
            .append(wrote)
            .append(" octets")
            .toString());
      }
      return wrote;
    }

    private static int writeASCII(
      final SeekableByteChannel channel,
      final String text)
      throws IOException
    {
      final var data = text.getBytes(StandardCharsets.US_ASCII);
      return writeChecked(channel, ByteBuffer.wrap(data), data.length);
    }

    private static int writeChunkID(
      final SeekableByteChannel channel,
      final RiffChunkID id)
      throws IOException
    {
      return writeASCII(channel, id.value());
    }

    private int writeUnsigned8(
      final SeekableByteChannel channel,
      final int x)
      throws IOException
    {
      return writeChecked(
        channel,
        ByteBuffer.allocate(1)
          .order(this.description.byteOrder())
          .put(0, (byte) (x & 0xff)),
        1);
    }

    private int writeUnsigned32(
      final SeekableByteChannel channel,
      final long x)
      throws IOException
    {
      return writeChecked(
        channel,
        ByteBuffer.allocate(4)
          .order(this.description.byteOrder())
          .putInt(0, (int) (x & 0xffffffffL)),
        4);
    }

    @Override
    public void write()
      throws RiffWriteException
    {
      try {
        this.root_channel.position(0L);

        final var chunks =
          this.description.linearizedChunks()
            .collect(Collectors.toList());

        LOG.trace("writing data");

        for (final var chunk : chunks) {
          this.writeChunk(this.root_channel, chunk);
        }

        LOG.trace("updating offsets");

        for (final var chunk : chunks) {
          final var offset =
            this.sizes_offsets.get(Long.valueOf(chunk.ordinal())).longValue();
          final var size =
            this.evaluateDataSizeOfChunk(chunk);

          LOG.trace(
            "[{}:{}]: offset 0x{}",
            chunk.id().value(),
            Long.valueOf(chunk.ordinal()),
            Long.toUnsignedString(offset, 16));
          LOG.trace(
            "[{}:{}]: size   {}",
            chunk.id().value(),
            Long.valueOf(chunk.ordinal()),
            size);

          this.root_channel.position(offset);
          this.writeUnsigned32(this.root_channel, size.sizeUnpadded());
        }

      } catch (final Exception e) {
        throw new RiffWriteException(e, this.source, position(this.root_channel));
      }
    }

    private RiffSize evaluateDataSizeOfChunk(
      final RiffFileWriterChunkDescriptionType chunk)
    {
      if (chunk.subChunks().isEmpty()) {
        return this.evaluateDataSizeOfChunkWithoutSubchunks(chunk);
      }

      return this.evaluateDataSizeOfChunkWithSubchunks(chunk);
    }

    private RiffSize evaluateDataSizeOfChunkWithSubchunks(
      final RiffFileWriterChunkDescriptionType chunk)
    {
      Preconditions.checkPrecondition(
        !chunk.subChunks().isEmpty(), "Chunk must have subchunks");

      final var subchunks_size =
        chunk.subChunks()
          .stream()
          .map(this::evaluateTotalSizeOfChunk)
          .mapToLong(RiffSize::size)
          .sum();

      final var subchunks_and_form_size =
        chunk.form().isPresent() ? Math.addExact(subchunks_size, FORM_OCTETS) : subchunks_size;

      return RiffSize.of(subchunks_and_form_size, false);
    }

    private RiffSize evaluateDataSizeOfChunkWithoutSubchunks(
      final RiffFileWriterChunkDescriptionType chunk)
    {
      Preconditions.checkPrecondition(
        chunk.subChunks().isEmpty(), "Chunk must not have subchunks");

      return this.sizes_written.getOrDefault(
        Long.valueOf(chunk.ordinal()),
        RiffSizes.padIfNecessary(0L));
    }

    private RiffSize evaluateTotalSizeOfChunkWithoutSubchunks(
      final RiffFileWriterChunkDescriptionType chunk)
    {
      Preconditions.checkPrecondition(
        chunk.subChunks().isEmpty(), "Chunk must not have subchunks");

      final var data_size =
        this.evaluateDataSizeOfChunkWithoutSubchunks(chunk);
      final var total_size =
        Math.addExact(HEADER_SIZE, data_size.size());

      return RiffSize.of(total_size, false);
    }

    private RiffSize evaluateTotalSizeOfChunk(
      final RiffFileWriterChunkDescriptionType chunk)
    {
      if (chunk.subChunks().isEmpty()) {
        return this.evaluateTotalSizeOfChunkWithoutSubchunks(chunk);
      }

      return this.evaluateTotalSizeOfChunkWithSubchunks(chunk);
    }

    private RiffSize evaluateTotalSizeOfChunkWithSubchunks(
      final RiffFileWriterChunkDescriptionType chunk)
    {
      Preconditions.checkPrecondition(
        !chunk.subChunks().isEmpty(), "Chunk must have subchunks");

      final var data_size =
        this.evaluateDataSizeOfChunkWithSubchunks(chunk);
      final var total_size =
        Math.addExact(HEADER_SIZE, data_size.size());

      return RiffSize.of(total_size, false);
    }

    private void writeChunk(
      final SeekableByteChannel base,
      final RiffFileWriterChunkDescriptionType chunk)
      throws IOException
    {
      try (var channel = RiffRelativeSeekableByteChannel.create(base, base.position(), false)) {
        writeChunkID(channel, chunk.id());

        this.sizes_offsets.put(
          Long.valueOf(chunk.ordinal()),
          Long.valueOf(position(this.root_channel)));
        this.writeUnsigned32(channel, 0L);

        final var form_option = chunk.form();
        if (form_option.isPresent()) {
          writeASCII(channel, form_option.get());
        }

        final var data_writer_opt = chunk.dataWriter();
        if (data_writer_opt.isPresent()) {
          this.writeChunkData(chunk, data_writer_opt);
        }
      }
    }

    private void writeChunkData(
      final RiffFileWriterChunkDescriptionType chunk,
      final Optional<RiffChunkDataWriterType> data_writer_opt)
      throws IOException
    {
      final var position_then = position(this.root_channel);
      LOG.trace(
        "[{}]: starting at 0x{}",
        chunk.id().value(),
        Long.toUnsignedString(position_then, 16));

      final var data_writer = data_writer_opt.get();
      final var declared_size = chunk.declaredSize();
      try (var data_channel = this.dataChannelFor(declared_size)) {
        if (declared_size.isPresent()) {
          data_channel.position(declared_size.getAsLong() - 1L);
          this.writeUnsigned8(data_channel, 0x00);
          data_channel.position(0L);
        }

        try {
          data_writer.write(data_channel);
        } catch (final RiffOutOfBoundsException e) {
          final var separator = System.lineSeparator();
          throw new IOException(
            new StringBuilder(128)
              .append("Data writer for chunk attempted an out-of-bounds write.")
              .append(separator)
              .append("  Chunk:        ")
              .append(chunk.id().value())
              .append(separator)
              .append("  Chunk offset: 0x")
              .append(Long.toUnsignedString(position_then, 16))
              .append(separator)
              .toString(), e);
        }

        if (declared_size.isPresent()) {
          data_channel.position(declared_size.getAsLong());
        }
      }

      final var position_now = position(this.root_channel);
      final var size = Math.subtractExact(position_now, position_then);

      LOG.trace(
        "[{}]: wrote {}",
        chunk.id().value(),
        Long.valueOf(size));

      this.sizes_written.put(Long.valueOf(chunk.ordinal()), RiffSizes.padIfNecessary(size));

      if (position_now % 2L != 0L) {
        this.writeUnsigned8(this.root_channel, 0x00);
        LOG.trace("[{}]: added padding byte", chunk.id().value());
      }
    }

    private SeekableByteChannel dataChannelFor(
      final OptionalLong declared_size)
      throws IOException
    {
      final var lower = this.root_channel.position();
      if (declared_size.isPresent()) {
        final var upper = lower + declared_size.getAsLong();
        return RiffRestrictedSeekableByteChannel.create(this.root_channel, lower, upper, false);
      }
      return RiffRelativeSeekableByteChannel.create(this.root_channel, lower, false);
    }
  }
}
