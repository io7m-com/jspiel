/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
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

import com.io7m.jspiel.api.RiffOutOfBoundsException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * A byte channel that can only access a specified range of a given underlying channel.
 */

public final class RiffRestrictedSeekableByteChannel implements SeekableByteChannel
{
  private final SeekableByteChannel delegate;
  private final long lower;
  private final long upper;
  private final boolean allow_close;
  private final long upper_relative;
  private long position_relative;
  private boolean closed;

  private RiffRestrictedSeekableByteChannel(
    final SeekableByteChannel in_channel,
    final long in_lower,
    final long in_upper,
    final boolean in_allow_close)
  {
    if (Long.compareUnsigned(in_lower, in_upper) >= 0) {
      throw new IllegalArgumentException("Invalid bounds (lower must be < upper)");
    }

    this.delegate = Objects.requireNonNull(in_channel, "channel");
    this.lower = in_lower;
    this.upper = in_upper;
    this.upper_relative = Math.subtractExact(this.upper, this.lower);

    this.position_relative = 0L;
    this.allow_close = in_allow_close;
  }

  /**
   * Create a new restricted seekable byte channel.
   *
   * @param in_channel     The underlying byte channel
   * @param in_lower       The lower bound of the accessible range
   * @param in_upper       The upper bound of the accessible range
   * @param in_allow_close {@code true} if closing this channel closes the underlying channel
   *
   * @return A new channel
   */

  public static SeekableByteChannel create(
    final SeekableByteChannel in_channel,
    final long in_lower,
    final long in_upper,
    final boolean in_allow_close)
  {
    return new RiffRestrictedSeekableByteChannel(in_channel, in_lower, in_upper, in_allow_close);
  }

  @Override
  public int read(final ByteBuffer dst)
    throws IOException
  {
    this.checkNotClosed();

    final var count = Integer.toUnsignedLong(dst.remaining());
    final var new_upper = Math.addExact(this.position_relative, count);
    if (Long.compareUnsigned(new_upper, this.upper_relative) >= 0) {
      final var separator = System.lineSeparator();
      throw new RiffOutOfBoundsException(
        new StringBuilder(128)
          .append("Attempted to read outside of the bounds of a restricted byte channel.")
          .append(separator)
          .append("  Attempted range: [")
          .append(Long.toUnsignedString(this.position_relative))
          .append(", ")
          .append(Long.toUnsignedString(new_upper))
          .append(')')
          .append(separator)
          .append("  Permitted range: [0, ")
          .append(Long.toUnsignedString(this.upper_relative))
          .append(')')
          .append(separator)
          .toString());
    }

    this.position(this.position());
    final var r = this.delegate.read(dst);
    this.position_relative = Math.addExact(this.position_relative, Integer.toUnsignedLong(r));
    return r;
  }

  private void checkNotClosed()
    throws ClosedChannelException
  {
    if (!this.isOpen()) {
      throw new ClosedChannelException();
    }
  }

  @Override
  public int write(final ByteBuffer src)
    throws IOException
  {
    this.checkNotClosed();

    final var count = Integer.toUnsignedLong(src.remaining());
    final var new_upper = Math.addExact(this.position_relative, count);
    if (Long.compareUnsigned(new_upper, this.upper_relative) > 0) {
      final var separator = System.lineSeparator();
      throw new RiffOutOfBoundsException(
        new StringBuilder(128)
          .append("Attempted to write outside of the bounds of a restricted byte channel.")
          .append(separator)
          .append("  Attempted range: [")
          .append(Long.toUnsignedString(this.position_relative))
          .append(", ")
          .append(Long.toUnsignedString(new_upper))
          .append(')')
          .append(separator)
          .append("  Permitted range: [0, ")
          .append(Long.toUnsignedString(this.upper_relative))
          .append(')')
          .append(separator)
          .toString());
    }

    this.position(this.position());
    final var r = this.delegate.write(src);
    this.position_relative = Math.addExact(this.position_relative, Integer.toUnsignedLong(r));
    return r;
  }

  @Override
  public long position()
    throws IOException
  {
    this.checkNotClosed();
    return this.position_relative;
  }

  @Override
  public SeekableByteChannel position(
    final long new_position)
    throws IOException
  {
    this.checkNotClosed();

    this.position_relative = new_position;
    this.delegate.position(Math.addExact(this.lower, this.position_relative));
    return this;
  }

  @Override
  public long size()
    throws IOException
  {
    this.checkNotClosed();
    return Math.subtractExact(this.upper, this.lower);
  }

  @Override
  public SeekableByteChannel truncate(
    final long size)
    throws IOException
  {
    this.checkNotClosed();
    throw new IllegalArgumentException("Cannot truncate a restricted byte channel");
  }

  @Override
  public boolean isOpen()
  {
    return !this.closed && this.delegate.isOpen();
  }

  @Override
  public void close()
    throws IOException
  {
    try {
      if (this.isOpen()) {
        if (this.allow_close) {
          this.delegate.close();
        }
      }
    } finally {
      this.closed = true;
    }
  }
}
