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

import com.io7m.jspiel.api.RiffBuilderException;
import com.io7m.jspiel.api.RiffChunkBuilderType;
import com.io7m.jspiel.api.RiffChunkDataWriterType;
import com.io7m.jspiel.api.RiffChunkID;
import com.io7m.jspiel.api.RiffFileBuilderProviderType;
import com.io7m.jspiel.api.RiffFileBuilderType;
import com.io7m.jspiel.api.RiffFileWriterChunkDescriptionType;
import com.io7m.jspiel.api.RiffFileWriterDescriptionType;
import org.osgi.service.component.annotations.Component;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A provider of file builders.
 */

@Component
public final class RiffFileBuilders implements RiffFileBuilderProviderType
{
  /**
   * Construct a provider.
   */

  public RiffFileBuilders()
  {

  }

  @Override
  public RiffFileBuilderType create(final ByteOrder order)
  {
    return new FileBuilder(Objects.requireNonNull(order, "order"));
  }

  private static final class FileBuilder implements RiffFileBuilderType
  {
    private ByteOrder order;
    private Optional<ChunkBuilder> root;

    FileBuilder(
      final ByteOrder in_order)
    {
      this.order = Objects.requireNonNull(in_order, "order");
    }

    private static ChunkDescription collect(
      final ChunkBuilder current)
    {
      return new ChunkDescription(
        null,
        Optional.empty(),
        current.id,
        current.form,
        current.size,
        current.children.stream()
          .map(FileBuilder::collect)
          .collect(Collectors.toList()),
        current.data_writer);
    }

    @Override
    public RiffFileBuilderType setOrder(
      final ByteOrder in_order)
    {
      this.order = Objects.requireNonNull(in_order, "order");
      return this;
    }

    @Override
    public RiffChunkBuilderType setRootChunk(
      final RiffChunkID id,
      final String form)
    {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(form, "form");

      final var new_root =
        new ChunkBuilder(this, Optional.empty(), id, Optional.of(form));
      this.root = Optional.of(new_root);
      return new_root;
    }

    @Override
    public RiffFileWriterDescriptionType build()
      throws RiffBuilderException
    {
      if (!this.root.isPresent()) {
        throw new RiffBuilderException("No root chunk was provided");
      }

      final var current_root = this.root.get();
      if (!current_root.closed) {
        throw new RiffBuilderException("A chunk builder has not been closed");
      }

      return new Description(this.order, collect(current_root));
    }

    private static final class Description implements RiffFileWriterDescriptionType
    {
      private final ByteOrder order;
      private final ChunkDescription root;

      Description(
        final ByteOrder in_order,
        final ChunkDescription in_root)
      {
        this.order =
          Objects.requireNonNull(in_order, "order");
        this.root =
          Objects.requireNonNull(in_root, "root");

        this.initializeParents(new AtomicLong(0L), null, this.root);
      }

      private static Stream<RiffFileWriterChunkDescriptionType> collect(
        final RiffFileWriterChunkDescriptionType node)
      {
        return Stream.concat(
          Stream.of(node),
          node.subChunks()
            .stream()
            .flatMap(Description::collect));
      }

      private void initializeParents(
        final AtomicLong ordinal_current,
        final ChunkDescription parent,
        final ChunkDescription current)
      {
        current.ordinal = ordinal_current.getAndIncrement();
        current.owner = this;
        current.parent = Optional.ofNullable(parent);
        current.children.forEach(child -> this.initializeParents(ordinal_current, current, child));
      }

      @Override
      public String toString()
      {
        return new StringBuilder(64)
          .append("[RiffFileWriterDescription ")
          .append(this.root.id.value())
          .append(' ')
          .append(this.order)
          .append(']')
          .toString();
      }

      @Override
      public ByteOrder byteOrder()
      {
        return this.order;
      }

      @Override
      public RiffFileWriterChunkDescriptionType rootChunk()
      {
        return this.root;
      }

      @Override
      public Stream<RiffFileWriterChunkDescriptionType> linearizedChunks()
      {
        return collect(this.root);
      }
    }

    private static final class ChunkDescription implements RiffFileWriterChunkDescriptionType
    {
      private final List<RiffFileWriterChunkDescriptionType> children_read;
      private final RiffChunkID id;
      private final Optional<String> form;
      private final OptionalLong size;
      private final List<ChunkDescription> children;
      private Description owner;
      private Optional<ChunkDescription> parent;
      private long ordinal;
      private Optional<RiffChunkDataWriterType> data_writer;

      ChunkDescription(
        final Description in_owner,
        final Optional<ChunkDescription> in_parent,
        final RiffChunkID in_id,
        final Optional<String> in_form,
        final OptionalLong in_size,
        final List<ChunkDescription> in_children,
        final Optional<RiffChunkDataWriterType> in_data_writer)
      {
        this.owner = in_owner;
        this.parent =
          Objects.requireNonNull(in_parent, "parent");
        this.id =
          Objects.requireNonNull(in_id, "id");
        this.form =
          Objects.requireNonNull(in_form, "form");
        this.size =
          Objects.requireNonNull(in_size, "size");
        this.children =
          Objects.requireNonNull(in_children, "children");
        this.data_writer =
          Objects.requireNonNull(in_data_writer, "data_writer");
        this.children_read =
          Collections.unmodifiableList(cast(this.children));
      }

      @SuppressWarnings("unchecked")
      private static <T, U extends T> List<T> cast(final List<U> xs)
      {
        return (List<T>) xs;
      }

      @Override
      public String toString()
      {
        return new StringBuilder(128)
          .append("[Chunk ")
          .append(this.id.value())
          .append(']')
          .toString();
      }

      @Override
      public long ordinal()
      {
        return this.ordinal;
      }

      @Override
      public RiffFileWriterDescriptionType file()
      {
        return Objects.requireNonNull(this.owner, "owner");
      }

      @Override
      public Optional<RiffFileWriterChunkDescriptionType> parent()
      {
        return this.parent.map(Function.identity());
      }

      @Override
      public RiffChunkID id()
      {
        return this.id;
      }

      @Override
      public OptionalLong declaredSize()
      {
        return this.size;
      }

      @Override
      public List<RiffFileWriterChunkDescriptionType> subChunks()
      {
        return this.children_read;
      }

      @Override
      public Optional<String> form()
      {
        return this.form;
      }

      @Override
      public Optional<RiffChunkDataWriterType> dataWriter()
      {
        return this.data_writer;
      }
    }

    private static final class ChunkBuilder implements RiffChunkBuilderType
    {
      private final FileBuilder file_builder;
      private final Optional<ChunkBuilder> parent;
      private final ArrayList<ChunkBuilder> children;
      private boolean closed;
      private RiffChunkID id;
      private Optional<String> form;
      private OptionalLong size;
      private Optional<RiffChunkDataWriterType> data_writer;

      ChunkBuilder(
        final FileBuilder in_file_builder,
        final Optional<ChunkBuilder> in_parent,
        final RiffChunkID in_id,
        final Optional<String> in_form)
      {
        this.file_builder =
          Objects.requireNonNull(in_file_builder, "file_builder");
        this.parent =
          Objects.requireNonNull(in_parent, "parent");
        this.id =
          Objects.requireNonNull(in_id, "id");
        this.form =
          Objects.requireNonNull(in_form, "form");

        this.size = OptionalLong.empty();
        this.children = new ArrayList<>(16);
        this.data_writer = Optional.empty();
      }

      @Override
      public void close()
        throws IllegalStateException
      {
        this.closed = true;
      }

      @Override
      public RiffChunkBuilderType setSize(
        final OptionalLong new_size)
      {
        this.size = new_size;
        return this;
      }

      @Override
      public RiffChunkBuilderType setSize(
        final long new_size)
      {
        this.size = OptionalLong.of(new_size);
        return this;
      }

      @Override
      public RiffChunkBuilderType setID(
        final RiffChunkID new_id)
      {
        this.id = Objects.requireNonNull(new_id, "id");
        return this;
      }

      @Override
      public RiffChunkBuilderType setForm(
        final String new_form)
      {
        this.form = Optional.of(Objects.requireNonNull(new_form, "form"));
        return this;
      }

      @Override
      public RiffChunkBuilderType setForm(
        final Optional<String> new_form)
      {
        this.form = Objects.requireNonNull(new_form, "form");
        return this;
      }

      @Override
      public RiffChunkBuilderType setDataWriter(
        final RiffChunkDataWriterType writer)
        throws IllegalStateException
      {
        Objects.requireNonNull(writer, "writer");

        if (!this.children.isEmpty()) {
          throw new IllegalStateException("A chunk cannot have both subchunks and data");
        }

        this.data_writer = Optional.of(writer);
        return this;
      }

      @Override
      public RiffChunkBuilderType addSubChunk(
        final RiffChunkID sub_id)
      {
        Objects.requireNonNull(sub_id, "id");

        if (this.data_writer.isPresent()) {
          throw new IllegalStateException("A chunk cannot have both subchunks and data");
        }

        final var sub_builder =
          new ChunkBuilder(
            this.file_builder,
            Optional.of(this),
            sub_id,
            Optional.empty());

        this.children.add(sub_builder);
        return sub_builder;
      }
    }
  }
}
