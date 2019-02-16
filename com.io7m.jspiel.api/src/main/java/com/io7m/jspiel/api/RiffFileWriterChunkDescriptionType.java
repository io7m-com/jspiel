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
import java.util.OptionalLong;

/**
 * A description of a RIFF chunk to be built.
 */

public interface RiffFileWriterChunkDescriptionType
{
  /**
   * Retrieve the ordinal number of the chunk. The ordinal number is essentially a number that
   * indicates in what order the chunk will appear in the output file. For any given chunk,
   * the ordinal number is guaranteed to be greater than that of the parent chunk's ordinal, and
   * is guaranteed to be greater than that of any previous sibling chunks. The ordinal number
   * is also guaranteed to be unique within a file.
   *
   * @return The ordinal number of the chunk
   */

  long ordinal();

  /**
   * @return The file to which this chunk belongs
   */

  RiffFileWriterDescriptionType file();

  /**
   * @return The parent chunk of this chunk, if any
   */

  Optional<RiffFileWriterChunkDescriptionType> parent();

  /**
   * @return The ID of the chunk
   */

  RiffChunkID id();

  /**
   * @return The declared size of the chunk, if any
   */

  OptionalLong declaredSize();

  /**
   * @return The list of subchunks of this chunk
   */

  List<RiffFileWriterChunkDescriptionType> subChunks();

  /**
   * @return The chunk form, if any
   */

  Optional<String> form();

  /**
   * @return The supplier of data for the chunk
   */

  Optional<RiffChunkDataWriterType> dataWriter();
}
