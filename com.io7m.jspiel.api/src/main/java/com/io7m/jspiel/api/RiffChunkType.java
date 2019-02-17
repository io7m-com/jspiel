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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   * @return The size in octets of the data in this chunk, including any form field (if present)
   */

  RiffSize dataSizeIncludingForm();

  /**
   * @return The total size of this chunk, including the data (and padding), chunk ID, and chunk
   * size field
   */

  default long totalSize()
  {
    return Math.addExact(this.dataSizeIncludingForm().size(), 8L);
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
    return Math.addExact(this.offset(), 8L);
  }

  /**
   * @return The linearized subchunks, including all descendants, in depth-first order
   */

  default Stream<RiffChunkType> linearizedDescendantChunks()
  {
    return Stream.concat(
      Stream.of(this),
      this.subChunks().stream().flatMap(RiffChunkType::linearizedDescendantChunks));
  }

  /**
   * @return The size in octets of the data in this chunk, excluding any form field (if present)
   */

  default RiffSize dataSizeExcludingForm()
  {
    return this.formType()
      .map(ignored -> {
        final var size = this.dataSizeIncludingForm();
        return size.withSize(Math.subtractExact(size.size(), 4L));
      }).orElse(this.dataSizeIncludingForm());
  }

  /**
   * Find a required subchunk as a direct subchunk of the current chunk. The first matching chunk is
   * returned - subsequent matching chunks are ignored.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunk
   *
   * @throws RiffRequiredChunkMissingException If no subchunk exists with the given ID and form
   */

  default RiffChunkType findRequiredSubChunkWithForm(
    final RiffChunkID id,
    final String form)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");

    return this.subChunks()
      .stream()
      .filter(subchunk -> subchunk.matchesWithForm(id, form))
      .findFirst()
      .orElseThrow(() -> {
        final var separator = System.lineSeparator();
        return new RiffRequiredChunkMissingException(
          new StringBuilder(128)
            .append("Required chunk not found.")
            .append(separator)
            .append("  Expected: ")
            .append(id.value())
            .append(" (form ")
            .append(form)
            .append(") as a subchunk of ")
            .append(this.name().value())
            .append(separator)
            .append("  Received: Nothing")
            .append(separator)
            .toString());
      });
  }

  /**
   * Find a required subchunk as a direct subchunk of the current chunk. The first matching chunk is
   * returned - subsequent matching chunks are ignored.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunk
   *
   * @throws RiffRequiredChunkMissingException If no subchunk exists with the given ID and form
   */

  default RiffChunkType findRequiredSubChunkWithForm(
    final String id,
    final String form)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return this.findRequiredSubChunkWithForm(RiffChunkID.of(id), form);
  }

  /**
   * Find required subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned, and the function raises an exception if no matching chunks are found.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunks
   *
   * @throws RiffRequiredChunkMissingException If no subchunks exist with the given ID and form
   */

  default List<RiffChunkType> findRequiredSubChunksWithForm(
    final RiffChunkID id,
    final String form)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");

    final var results =
      this.subChunks()
        .stream()
        .filter(subchunk -> subchunk.matchesWithForm(id, form))
        .collect(Collectors.toList());

    if (results.isEmpty()) {
      final var separator = System.lineSeparator();
      throw new RiffRequiredChunkMissingException(
        new StringBuilder(128)
          .append("Required chunks not found.")
          .append(separator)
          .append("  Expected: At least one ")
          .append(id.value())
          .append(" (form ")
          .append(form)
          .append(") as a subchunk of ")
          .append(this.name().value())
          .append(separator)
          .append("  Received: Nothing")
          .append(separator)
          .toString());
    }

    return results;
  }

  /**
   * Find required subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned, and the function raises an exception if no matching chunks are found.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunks
   *
   * @throws RiffRequiredChunkMissingException If no subchunks exist with the given ID and form
   */

  default List<RiffChunkType> findRequiredSubChunksWithForm(
    final String id,
    final String form)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return this.findRequiredSubChunksWithForm(RiffChunkID.of(id), form);
  }

  /**
   * Find a required subchunk as a direct subchunk of the current chunk. The first matching chunk is
   * returned - subsequent matching chunks are ignored.
   *
   * @param id The required ID
   *
   * @return The chunk
   *
   * @throws RiffRequiredChunkMissingException If no subchunk exists with the given ID
   */

  default RiffChunkType findRequiredSubChunk(
    final RiffChunkID id)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");

    return this.subChunks()
      .stream()
      .filter(subchunk -> subchunk.matches(id))
      .findFirst()
      .orElseThrow(() -> {
        final var separator = System.lineSeparator();
        return new RiffRequiredChunkMissingException(
          new StringBuilder(128)
            .append("Required chunk not found.")
            .append(separator)
            .append("  Expected: ")
            .append(id.value())
            .append(" as a subchunk of ")
            .append(this.name().value())
            .append(separator)
            .append("  Received: Nothing")
            .append(separator)
            .toString());
      });
  }

  /**
   * Find a required subchunk as a direct subchunk of the current chunk. The first matching chunk is
   * returned - subsequent matching chunks are ignored.
   *
   * @param id The required ID
   *
   * @return The chunk
   *
   * @throws RiffRequiredChunkMissingException If no subchunk exists with the given ID
   */

  default RiffChunkType findRequiredSubChunk(
    final String id)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");
    return this.findRequiredSubChunk(RiffChunkID.of(id));
  }

  /**
   * Find required subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned, and the function raises an exception if no matching chunks are found.
   *
   * @param id The required ID
   *
   * @return The chunks
   *
   * @throws RiffRequiredChunkMissingException If no subchunks exist with the given ID and form
   */

  default List<RiffChunkType> findRequiredSubChunks(
    final RiffChunkID id)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");

    final var results =
      this.subChunks()
        .stream()
        .filter(subchunk -> subchunk.matches(id))
        .collect(Collectors.toList());

    if (results.isEmpty()) {
      final var separator = System.lineSeparator();
      throw new RiffRequiredChunkMissingException(
        new StringBuilder(128)
          .append("Required chunks not found.")
          .append(separator)
          .append("  Expected: At least one ")
          .append(id.value())
          .append(" as a subchunk of ")
          .append(this.name().value())
          .append(separator)
          .append("  Received: Nothing")
          .append(separator)
          .toString());
    }

    return results;
  }

  /**
   * Find required subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned, and the function raises an exception if no matching chunks are found.
   *
   * @param id The required ID
   *
   * @return The chunks
   *
   * @throws RiffRequiredChunkMissingException If no subchunks exist with the given ID and form
   */

  default List<RiffChunkType> findRequiredSubChunks(
    final String id)
    throws RiffRequiredChunkMissingException
  {
    Objects.requireNonNull(id, "id");
    return this.findRequiredSubChunks(RiffChunkID.of(id));
  }

  /**
   * Find an optional subchunk as a direct subchunk of the current chunk. The first matching chunk
   * is returned - subsequent matching chunks are ignored.
   *
   * @param id The required ID
   *
   * @return The chunk
   */

  default Optional<RiffChunkType> findOptionalSubChunk(
    final RiffChunkID id)
  {
    return this.findOptionalSubChunks(id).findFirst();
  }

  /**
   * Find an optional subchunk as a direct subchunk of the current chunk. The first matching chunk
   * is returned - subsequent matching chunks are ignored.
   *
   * @param id The required ID
   *
   * @return The chunk
   */

  default Optional<RiffChunkType> findOptionalSubChunk(
    final String id)
  {
    return this.findOptionalSubChunks(id).findFirst();
  }

  /**
   * Find optional subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned.
   *
   * @param id The required ID
   *
   * @return The chunks
   */

  default Stream<RiffChunkType> findOptionalSubChunks(
    final RiffChunkID id)
  {
    return this.subChunks()
      .stream()
      .filter(subchunk -> subchunk.matches(id));
  }

  /**
   * Find optional subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned.
   *
   * @param id The required ID
   *
   * @return The chunks
   */

  default Stream<RiffChunkType> findOptionalSubChunks(
    final String id)
  {
    return this.findOptionalSubChunks(RiffChunkID.of(id));
  }

  /**
   * Find an optional subchunk as a direct subchunk of the current chunk. The first matching chunk
   * is returned - subsequent matching chunks are ignored.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunk
   */

  default Optional<RiffChunkType> findOptionalSubChunkWithForm(
    final RiffChunkID id,
    final String form)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return this.findOptionalSubChunksWithForm(id, form).findFirst();
  }

  /**
   * Find an optional subchunk as a direct subchunk of the current chunk. The first matching chunk
   * is returned - subsequent matching chunks are ignored.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunk
   */

  default Optional<RiffChunkType> findOptionalSubChunkWithForm(
    final String id,
    final String form)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return this.findOptionalSubChunksWithForm(RiffChunkID.of(id), form).findFirst();
  }

  /**
   * Find optional subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunk
   */

  default Stream<RiffChunkType> findOptionalSubChunksWithForm(
    final RiffChunkID id,
    final String form)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");

    return this.subChunks()
      .stream()
      .filter(subchunk -> subchunk.matchesWithForm(id, form));
  }

  /**
   * Find optional subchunks as direct subchunks of the current chunk. All matching chunks are
   * returned.
   *
   * @param id   The required ID
   * @param form The required form
   *
   * @return The chunk
   */

  default Stream<RiffChunkType> findOptionalSubChunksWithForm(
    final String id,
    final String form)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return this.findOptionalSubChunksWithForm(RiffChunkID.of(id), form);
  }

  /**
   * @param id A chunk ID
   *
   * @return {@code true} if the current chunk has the given ID
   */

  default boolean matches(
    final String id)
  {
    Objects.requireNonNull(id, "id");
    return this.matches(RiffChunkID.of(id));
  }

  /**
   * @param id A chunk ID
   *
   * @return {@code true} if the current chunk has the given ID
   */

  default boolean matches(
    final RiffChunkID id)
  {
    Objects.requireNonNull(id, "id");
    return Objects.equals(this.name(), id);
  }

  /**
   * @param id   A chunk ID
   * @param form A chunk form
   *
   * @return {@code true} if the current chunk has the given ID
   */

  default boolean matchesWithForm(
    final RiffChunkID id,
    final String form)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return Objects.equals(this.name(), id) && Objects.equals(this.formType(), Optional.of(form));
  }

  /**
   * @param id   A chunk ID
   * @param form A chunk form
   *
   * @return {@code true} if the current chunk has the given ID
   */

  default boolean matchesWithForm(
    final String id,
    final String form)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(form, "form");
    return this.matchesWithForm(RiffChunkID.of(id), form);
  }
}
