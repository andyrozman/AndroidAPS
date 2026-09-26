package app.aaps.pump.common.test

import androidx.annotation.StringRes
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper

class PreviewAAPSLogger : AAPSLogger {

    override fun debug(message: String) {
        println("DEBUG: $message")
    }

    override fun debug(enable: Boolean, tag: LTag, message: String) {
        println("DEBUG: $message")
    }

    override fun debug(tag: LTag, message: String) {
        println("DEBUG: : " + tag.tag + " " + message)
    }

    override fun debug(tag: LTag, accessor: () -> String) {
        println("DEBUG: : " + tag.tag + " " + accessor.invoke())
    }

    override fun debug(tag: LTag, format: String, vararg arguments: Any?) {
        println("DEBUG: : " + tag.tag + " " + withArguments(format, arguments))
    }

    override fun warn(tag: LTag, message: String) {
        println("WARN: " + tag.tag + " " + message)
    }

    override fun warn(tag: LTag, format: String, vararg arguments: Any?) {
        println("INFO: : " + tag.tag + " " + withArguments(format, arguments))
    }

    override fun info(tag: LTag, message: String) {
        println("INFO: " + tag.tag + " " + message)
    }

    override fun info(tag: LTag, format: String, vararg arguments: Any?) {
        println("INFO: : " + tag.tag + " " + withArguments(format, arguments))
    }

    override fun error(tag: LTag, message: String) {
        println("ERROR: " + tag.tag + " " + message)
    }

    override fun error(message: String) {
        println("ERROR: $message")
    }

    override fun error(message: String, throwable: Throwable) {
        println("ERROR: $message $throwable")
    }

    override fun error(format: String, vararg arguments: Any?) {
        println("ERROR: : " + withArguments(format, arguments))
    }

    override fun error(tag: LTag, message: String, throwable: Throwable) {
        println("ERROR: " + tag.tag + " " + message + " " + throwable)
    }

    override fun error(tag: LTag, format: String, vararg arguments: Any?) {
        println("ERROR: : " + tag.tag + " " + withArguments(format, arguments))
    }

    override fun debug(className: String, methodName: String, lineNumber: Int, tag: LTag, message: String) {
        println("DEBUG: : ${tag.tag} $className.$methodName():$lineNumber $message")
    }

    override fun info(className: String, methodName: String, lineNumber: Int, tag: LTag, message: String) {
        println("INFO: : ${tag.tag} $className.$methodName():$lineNumber $message")
    }

    override fun warn(className: String, methodName: String, lineNumber: Int, tag: LTag, message: String) {
        println("WARN: : ${tag.tag} $className.$methodName():$lineNumber $message")
    }

    override fun error(className: String, methodName: String, lineNumber: Int, tag: LTag, message: String) {
        println("ERROR: : ${tag.tag} $className.$methodName():$lineNumber $message")
    }

    /**
     * Puts the arguments after the format string instead of substituting them into it.
     *
     * `String.format` is JVM only, and nothing needs it here: this logger only prints to stdout for
     * a person reading a failing test, and no test reads its output back. The old code was in any
     * case passing the whole `arguments` array as a single value rather than spreading it, so a
     * line with more than one placeholder never formatted correctly to begin with.
     */
    private fun withArguments(format: String, arguments: Array<out Any?>): String =
        if (arguments.isEmpty()) format else "$format ${arguments.joinToString()}"
}


class PreviewResourceHelper: ResourceHelper {

    fun addString(@StringRes resId: Int, value: String ) {

    }

    override fun gs(@StringRes id: Int): String {
        return "$id"
    }

    override fun gs(id: Int, vararg args: Any?): String {
        return "$id"
    }

    override fun gq(id: Int, quantity: Int, vararg args: Any?): String {
        TODO("Not yet implemented")
    }

    override fun gsNotLocalised(id: Int, vararg args: Any?): String {
        TODO("Not yet implemented")
    }

    override fun shortTextMode(): Boolean {
        return false
    }

}