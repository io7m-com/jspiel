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

import java.net.URI;
import java.util.Objects;

/**
 * The type of exceptions thrown by parsers.
 */

public final class RiffParseException extends RiffException
{
  private static final long serialVersionUID = -2367950306731803298L;
  private final URI source;
  private final long offset;

  /**
   * @return The source URI
   */

  public URI source()
  {
    return this.source;
  }

  /**
   * @return The offset in octets within the input that caused the error
   */

  public long offset()
  {
    return this.offset;
  }

  /**
   * Construct an exception.
   *
   * @param message   The message
   * @param in_offset The offset in octets within the input that caused the error
   * @param in_source The source URI
   */

  public RiffParseException(
    final String message,
    final URI in_source,
    final long in_offset)
  {
    super(message);
    this.source = Objects.requireNonNull(in_source, "source");
    this.offset = in_offset;
  }

  /**
   * Construct an exception.
   *
   * @param message   The message
   * @param cause     The underlying cause
   * @param in_offset The offset in octets within the input that caused the error
   * @param in_source The source URI
   */

  public RiffParseException(
    final String message,
    final Throwable cause,
    final URI in_source,
    final long in_offset)
  {
    super(message, cause);
    this.source = Objects.requireNonNull(in_source, "source");
    this.offset = in_offset;
  }

  /**
   * Construct an exception.
   *
   * @param cause     The underlying cause
   * @param in_offset The offset in octets within the input that caused the error
   * @param in_source The source URI
   */

  public RiffParseException(
    final Throwable cause,
    final URI in_source,
    final long in_offset)
  {
    super(cause);
    this.source = Objects.requireNonNull(in_source, "source");
    this.offset = in_offset;
  }
}
