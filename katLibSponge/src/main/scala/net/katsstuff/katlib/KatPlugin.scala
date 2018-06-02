/*
 * This file is part of KatLib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 Katrix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.katsstuff.katlib

import java.nio.file.Path

import scala.concurrent.ExecutionContext

import org.slf4j.Logger
import org.spongepowered.api.plugin.PluginContainer

/**
  * Base class for a Sponge kat plugin.
  */
trait KatPlugin {

  /**
    * The base SLF4J logger.
    */
  def logger: Logger

  /**
    * The place where configs and stuff is stored.
    */
  def configDir: Path

  /**
    * Our container instance.
    */
  def container: PluginContainer

  /**
    * An execution context for performing stuff on the main game thread.
    */
  def syncExecutionContext: ExecutionContext

  /**
    * An execution context for performing stuff away from the main game thread.
    */
  def asyncExecutionContext: ExecutionContext
}
