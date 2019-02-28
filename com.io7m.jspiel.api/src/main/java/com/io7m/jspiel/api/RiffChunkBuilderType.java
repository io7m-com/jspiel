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

import java.util.Optional;
import java.util.OptionalLong;

/**
 * A builder for RIFF chunks.
 *
 * It is an error to attempt to close a chunk without having called either of {@link
 * #addSubChunk(RiffChunkID)} or {@link #setDataWriter(RiffChunkDataWriterType)}.
 */

public interface RiffChunkBuilderType extends AutoCloseable
{
  @Override
  void close()
    throws IllegalStateException;

  /**
   * Set the declared size of the chunk.
   *
   * @param size The size
   *
   * @return The current builder
   */

  RiffChunkBuilderType setSize(
    OptionalLong size);

  /**
   * Set the declared size of the chunk.
   *
   * @param size The size
   *
   * @return The current builder
   */

  RiffChunkBuilderType setSize(
    long size);

  /**
   * Set the ID of the chunk.
   *
   * @param id The id
   *
   * @return The current builder
   */

  RiffChunkBuilderType setID(
    RiffChunkID id);

  /**
   * Set the form of the chunk.
   *
   * @param form The form
   *
   * @return The current builder
   */

  RiffChunkBuilderType setForm(
    String form);

  /**
   * Set the form of the chunk.
   *
   * @param form The form
   *
   * @return The current builder
   */

  RiffChunkBuilderType setForm(
    Optional<String> form);

  /**
   * Set the data writer for the chunk.
   *
   * @param writer The data writer
   *
   * @return The current builder
   *
   * @throws IllegalStateException If {@link #addSubChunk(RiffChunkID)} has already been called for
   *                               this chunk
   */

  RiffChunkBuilderType setDataWriter(
    RiffChunkDataWriterType writer)
    throws IllegalStateException;

  /**
   * Add a subchunk.
   *
   * @param id The id of the chunk
   *
   * @return A builder for the subchunk
   *
   * @throws IllegalStateException If {@link #setDataWriter(RiffChunkDataWriterType)} has already
   *                               been called for this chunk
   */

  RiffChunkBuilderType addSubChunk(
    RiffChunkID id)
    throws IllegalStateException;
}
