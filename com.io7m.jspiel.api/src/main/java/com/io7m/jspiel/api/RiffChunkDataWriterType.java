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

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * A data writer for a chunk.
 */

public interface RiffChunkDataWriterType
{
  /**
   * Called when a client is expected to write data to the given channel. The given channel is
   * configured such that the start of the chunk is at position {@code 0}. If the client declared
   * that the chunk must be a certain size, then attempting to write more data than the declared
   * size will cause the channel to raise an exception.
   *
   * @param data The writable channel
   *
   * @throws IOException On I/O errors
   */

  void write(SeekableByteChannel data)
    throws IOException;
}
