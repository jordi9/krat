package com.jordi9.krat.gag

/**
 * Marks a piece of code that deliberately breaks a clean code rule.
 * This is a way of saying "You Only Live Once" and acknowledging the trade-off.
 *
 * It is strongly encouraged to provide a reason for using this annotation.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.FIELD,
  AnnotationTarget.EXPRESSION,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.TYPE
)
annotation class YOLO(val reason: String = "No reason provided.")
