package com.atlassian.clover.test.junit

public class AntVersions {
  static String DEFAULT_VERSION = "1.9.4";

  static Closure SPLIT = { it.equals(null) ? null : it.split(",") }
  static Closure CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT = { it.equals(null) ? [AntVersions.DEFAULT_VERSION] : it.split(",") }
}
