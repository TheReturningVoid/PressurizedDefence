/*
 * Pressurized Defence - Steam-powered weaponry and defences in Minecraft.
 * Copyright (C) 2018  Jacob Juric
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.retvoid.pressurizeddefence.tile

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ITickable}
import net.minecraftforge.common.capabilities.Capability
import net.retvoid.pressurizeddefence.capability.Capabilities
import net.retvoid.pressurizeddefence.capability.steam.{ISteamHolder, SteamHolder}
import net.retvoid.pressurizeddefence.Predefs._

class TilePipe extends TileEntity with ITickable {
  private val steam: SteamHolder = new SteamHolder(200) {
    override def onSteamChange(prev: Int): Unit = markDirty()
  }

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
    if (capability == Capabilities.STEAM_CAPABILITY) true
    else super.hasCapability(capability, facing)
  }

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
    if (capability == Capabilities.STEAM_CAPABILITY) Capabilities.STEAM_CAPABILITY.cast(steam)
    else super.getCapability(capability, facing)
  }

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    super.readFromNBT(compound)
    if (compound.hasKey("steam")) steam.set(compound.getInteger("steam"))
  }

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound = {
    super.writeToNBT(compound)
    compound.setInteger("steam", steam.getSteam)
    compound
  }

  override def update(): Unit = {
    val touchingSteamContainers: Seq[ISteamHolder] = pos.allTouchingZipped
      .map { case (p, s) => (world.getTileEntity(p), s) }
      .filter { case (t, _) => t != null }
      .flatMap { case (t, s) => Option(t.getCapability(Capabilities.STEAM_CAPABILITY, s.getOpposite)) }
      .filter { _.acceptsSteam }
    val steamToTransfer: Int = if (touchingSteamContainers.isEmpty) 0 else steam.getSteam / touchingSteamContainers.size
    touchingSteamContainers foreach { h => {
      val amount: Int = if (steam.consume(steamToTransfer, true) + h.getSteam > h.getMaxSteam) h.getMaxSteam - h.getSteam
        else h.add(steam.consume(steamToTransfer, true), true)
      h.add(steam.consume(amount))
    }}
     if (touchingSteamContainers.nonEmpty) markDirty()
  }
}
