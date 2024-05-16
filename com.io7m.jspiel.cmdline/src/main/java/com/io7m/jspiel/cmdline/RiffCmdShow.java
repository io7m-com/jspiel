/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.jspiel.cmdline;

import com.io7m.jspiel.api.RiffChunkType;
import com.io7m.jspiel.api.RiffFileParserProviderType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.StandardOpenOption.READ;

/**
 * Display a RIFF file.
 */

public final class RiffCmdShow implements QCommandType
{
  private final QCommandMetadata metadata;

  private static final QParameterNamed1<Path> FILE =
    new QParameterNamed1<>(
      "--file",
      List.of(),
      new QStringType.QConstant("The configuration file."),
      Optional.empty(),
      Path.class
    );

  /**
   * Construct a command.
   */

  public RiffCmdShow()
  {
    this.metadata = new QCommandMetadata(
      "show",
      new QStringType.QConstant("Show the given RIFF file."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(List.of(FILE));
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    QLogback.configure(context);

    final var file =
      context.parameterValue(FILE);

    final var parsers =
      ServiceLoader.load(RiffFileParserProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No RIFF file parser service available"));

    try (var channel = FileChannel.open(file, READ)) {
      final var map =
        channel.map(READ_ONLY, 0L, channel.size());
      final var parser =
        parsers.createForByteBuffer(file.toUri(), map);
      final var riff =
        parser.parse();

      for (final var chunk : riff.chunks()) {
        showChunk(chunk, 0);
      }
    }

    return QCommandStatus.SUCCESS;
  }

  private static void showChunk(
    final RiffChunkType chunk,
    final int depth)
  {
    for (var i = 0; i < depth; ++i) {
      System.out.print("  ");
    }

    chunk.formType()
      .ifPresentOrElse(
        formType -> System.out.printf(
          "%s(%s) (size %d)\n",
          chunk.name().value(),
          formType,
          Long.valueOf(chunk.dataSizeExcludingForm().sizeUnpadded())),
        () -> System.out.printf(
          "%s (size %d)\n",
          chunk.name().value(),
          Long.valueOf(chunk.dataSizeExcludingForm().sizeUnpadded())));

    for (final var sub_chunk : chunk.subChunks()) {
      showChunk(sub_chunk, depth + 1);
    }
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
