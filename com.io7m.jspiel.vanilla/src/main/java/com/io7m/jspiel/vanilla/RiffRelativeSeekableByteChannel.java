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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * A byte channel that is offset by a fixed amount.
 */

public final class RiffRelativeSeekableByteChannel implements SeekableByteChannel
{
  private final SeekableByteChannel delegate;
  private final long lower;
  private final boolean allow_close;
  private long position;
  private boolean closed;

  private RiffRelativeSeekableByteChannel(
    final SeekableByteChannel in_channel,
    final long in_lower,
    final boolean in_allow_close)
  {
    this.delegate = Objects.requireNonNull(in_channel, "channel");
    this.lower = in_lower;
    this.position = 0L;
    this.allow_close = in_allow_close;
  }

  /**
   * Create a new restricted seekable byte channel.
   *
   * @param in_channel     The underlying byte channel
   * @param in_lower       The lower bound of the accessible range
   * @param in_allow_close {@code true} if closing this channel closes the underlying channel
   *
   * @return A new channel
   */

  public static SeekableByteChannel create(
    final SeekableByteChannel in_channel,
    final long in_lower,
    final boolean in_allow_close)
  {
    return new RiffRelativeSeekableByteChannel(in_channel, in_lower, in_allow_close);
  }

  @Override
  public int read(final ByteBuffer dst)
    throws IOException
  {
    this.checkNotClosed();

    this.position(this.position());
    final var r = this.delegate.read(dst);
    this.position = Math.addExact(this.position, Integer.toUnsignedLong(r));
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

    this.position(this.position());
    final var r = this.delegate.write(src);
    this.position = Math.addExact(this.position, Integer.toUnsignedLong(r));
    return r;
  }

  @Override
  public long position()
    throws IOException
  {
    this.checkNotClosed();
    return this.position;
  }

  @Override
  public SeekableByteChannel position(
    final long new_position)
    throws IOException
  {
    this.checkNotClosed();

    this.position = new_position;
    this.delegate.position(Math.addExact(this.lower, this.position));
    return this;
  }

  @Override
  public long size()
    throws IOException
  {
    this.checkNotClosed();
    return this.delegate.size();
  }

  @Override
  public SeekableByteChannel truncate(
    final long size)
    throws IOException
  {
    this.checkNotClosed();
    return this.delegate.truncate(size);
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
