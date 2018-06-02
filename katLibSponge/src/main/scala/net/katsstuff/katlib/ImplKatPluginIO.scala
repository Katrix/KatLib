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

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.SpongeExecutorService

import cats.effect.IO
import net.katsstuff.katlib.algebras.TextConversion
import net.katsstuff.katlib.impl._
import net.katstuff.katlib.algebras._
import net.katstuff.katlib.impl.{FileAccessImpl, LocalizedImpl}

abstract class ImplKatPluginIO(val logger: Logger, val configDir: Path, spongeContainer: PluginContainer)
    extends KatPlugin {

  implicit val plugin: KatPlugin = this

  implicit val T:             TextConversion[IO]                     = new SpongeTextConversion[IO]
  implicit val C:             Cache[IO]                              = new SpongeCache[IO]
  implicit val CS:            CommandSourceAccess[IO, CommandSource] = new SpongeCommandSourceOps[IO]
  implicit val FA:            FileAccess[IO]                         = new FileAccessImpl[IO]
  implicit val ExtractLocale: Localized[IO, CommandSource]           = new LocalizedImpl[IO, CommandSource]
  implicit val Log:           LogHelper[IO]                          = new SpongeLogHelper[IO]
  implicit val Page:          Pagination[IO, CommandSource]          = new SpongePagination[IO]
  implicit val P:             PlayerAccess[IO, Player, User]         = new SpongePlayerAccess[IO]
  implicit val G:             PluginGlobal[IO]                    = new SpongePluginGlobalIO

  val container: PluginContainer = spongeContainer

  lazy val syncExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Sponge.getScheduler.createSyncExecutor(this))

  lazy val asyncExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Sponge.getScheduler.createAsyncExecutor(this))
}
