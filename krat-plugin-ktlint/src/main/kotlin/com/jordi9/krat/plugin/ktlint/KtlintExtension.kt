package com.jordi9.krat.plugin.ktlint

import org.gradle.api.provider.Property

interface KtlintExtension {
  val version: Property<String>
}
