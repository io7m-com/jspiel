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

package com.io7m.jspiel.api;

import java.nio.channels.SeekableByteChannel;

/**
 * A provider of seekable byte channels.
 */

public interface RiffSeekableByteChannelsType
{
  /**
   * Create a restricted byte channel from the given existing channel. The returned byte channel is
   * limited to accessing data in the range {@code [lower, upper)} expressed as offsets in the base
   * channel. If the base channel was readable, the new channel will be readable. If the base
   * channel was writable, the new channel will be writable. The new channel is configured such that
   * a read/write from/to positions {@code [0, upper - lower]} in the new channel will equate to a
   * read/write to/from positions {@code [lower, upper]} in the base channel. Closing the new
   * channel will <i>not</i> close the base channel.
   *
   * @param channel The base channel
   * @param lower   The absolute, inclusive lower bound of the new channel
   * @param upper   The absolute, exclusive upper bound of the new channel
   *
   * @return A new byte channel
   */

  SeekableByteChannel createFromChannel(
    SeekableByteChannel channel,
    long lower,
    long upper);
}
