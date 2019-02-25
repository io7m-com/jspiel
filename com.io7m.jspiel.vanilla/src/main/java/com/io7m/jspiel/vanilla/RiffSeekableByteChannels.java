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

import com.io7m.jspiel.api.RiffSeekableByteChannelsType;
import org.osgi.service.component.annotations.Component;

import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * The default provider of seekable byte channels.
 */

@Component
public final class RiffSeekableByteChannels implements RiffSeekableByteChannelsType
{
  /**
   * Construct a provider.
   */

  public RiffSeekableByteChannels()
  {

  }

  @Override
  public SeekableByteChannel createFromChannel(
    final SeekableByteChannel channel,
    final long lower,
    final long upper)
  {
    Objects.requireNonNull(channel, "channel");
    return RiffRestrictedSeekableByteChannel.create(channel, lower, upper, false);
  }
}
