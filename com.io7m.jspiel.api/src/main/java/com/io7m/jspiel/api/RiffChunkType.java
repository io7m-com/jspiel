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

import java.util.List;
import java.util.Optional;

/**
 * A parsed RIFF chunk. Values of this type are effectively immutable.
 */

public interface RiffChunkType
{
  /**
   * @return The parent of this RIFF chunk, if one exists
   */

  Optional<RiffChunkType> parent();

  /**
   * @return The chunk ID
   */

  RiffChunkID name();

  /**
   * @return The absolute offset in octets of the start of this chunk within the RIFF file
   */

  long offset();

  /**
   * @return The size in octets of the data in this chunk
   */

  long dataSize();

  /**
   * @return The total size of this chunk, including the data, chunk ID, and chunk size field
   */

  default long totalSize()
  {
    return this.dataSize() + 8L;
  }

  /**
   * @return The form type of this chunk (only applicable to {@code RIFF} and {@code LIST} chunks).
   */

  Optional<String> formType();

  /**
   * @return A read-only list of subchunks within this chunk (only applicable to {@code RIFF} and
   * {@code LIST} chunks).
   */

  List<RiffChunkType> subChunks();

  /**
   * @return The absolute offset in octets of the start of this chunk's data within the RIFF file
   */

  default long dataOffset()
  {
    return this.offset() + 8L;
  }
}
