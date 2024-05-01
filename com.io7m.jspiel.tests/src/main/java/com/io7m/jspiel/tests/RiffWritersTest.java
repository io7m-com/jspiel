package com.io7m.jspiel.tests;

import com.io7m.jspiel.api.RiffFileBuilderProviderType;
import com.io7m.jspiel.api.RiffFileParserProviderType;
import com.io7m.jspiel.api.RiffFileWriterProviderType;
import com.io7m.jspiel.vanilla.RiffFileBuilders;
import com.io7m.jspiel.vanilla.RiffParsers;
import com.io7m.jspiel.vanilla.RiffWriters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RiffWritersTest extends RiffWritersContract
{
  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(RiffWritersTest.class);
  }

  @Override
  protected RiffFileParserProviderType parsers()
  {
    return new RiffParsers();
  }

  @Override
  protected RiffFileWriterProviderType writers()
  {
    return new RiffWriters();
  }

  @Override
  protected RiffFileBuilderProviderType builders()
  {
    return new RiffFileBuilders();
  }
}
