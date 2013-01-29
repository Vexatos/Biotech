package gigaherz.biotech.container;

import gigaherz.biotech.slots.emptyBucketSlot;
import gigaherz.biotech.slots.milkBucketSlot;
import gigaherz.biotech.tileentity.MilkingMachineTileEntity;
import gigaherz.biotech.tileentity.MilkingManagerTileEntity;
import gigaherz.biotech.tileentity.TillingMachineTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnace;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import universalelectricity.core.implement.IItemElectric;
import universalelectricity.prefab.SlotElectricItem;

public class MilkingMachineContainer extends Container
{
	private MilkingMachineTileEntity tileEntity;

	public MilkingMachineContainer(InventoryPlayer par1InventoryPlayer, MilkingMachineTileEntity tileEntity)
	{
		this.tileEntity = tileEntity;

		// Electric Input Slot
		this.addSlotToContainer(new SlotElectricItem(tileEntity, 0, 7, 16));

		int var3;

		for (var3 = 0; var3 < 1; ++var3)
		{
			for (int var4 = 0; var4 < 9; ++var4)
			{
				this.addSlotToContainer(new Slot(par1InventoryPlayer, var4 + var3 * 9 + 9, 8 + var4 * 18, 84 + var3 * 18));
			}
		}
		tileEntity.openChest();
	}

	public void onCraftGuiClosed(EntityPlayer entityplayer)
	{
		super.onCraftGuiClosed(entityplayer);
		tileEntity.closeChest();
	}

	@Override
	public boolean canInteractWith(EntityPlayer par1EntityPlayer)
	{
		return this.tileEntity.isUseableByPlayer(par1EntityPlayer);
	}

	/**
	 * Called to transfer a stack from one inventory to the other eg. when shift clicking.
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par1)
	{
		ItemStack var2 = null;
		Slot var3 = (Slot) this.inventorySlots.get(par1);

		if (var3 != null && var3.getHasStack())
		{
			ItemStack var4 = var3.getStack();
			var2 = var4.copy();

			if (par1 == 2)
			{
				if (!this.mergeItemStack(var4, 3, 39, true)) { return null; }

				var3.onSlotChange(var4, var2);
			}
			else if (par1 != 1 && par1 != 0)
			{
				if (var4.getItem() instanceof IItemElectric)
				{
					if (!this.mergeItemStack(var4, 0, 1, false)) { return null; }
				}
			}
			else if (!this.mergeItemStack(var4, 3, 39, false)) { return null; }

			if (var4.stackSize == 0)
			{
				var3.putStack((ItemStack) null);
			}
			else
			{
				var3.onSlotChanged();
			}

			if (var4.stackSize == var2.stackSize) { return null; }

			var3.onPickupFromSlot(par1EntityPlayer, var4);
		}

		return var2;
	}
}