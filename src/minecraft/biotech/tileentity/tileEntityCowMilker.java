package biotech.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;
import biotech.Biotech;
import biotech.handlers.PacketHandler;
import biotech.helpers.IPacketReceiver;

import com.google.common.io.ByteArrayDataInput;

public class tileEntityCowMilker extends tileEntityBasicMachine implements IPacketReceiver, ITankContainer
{
	protected List<EntityCow>	CowList					= new ArrayList<EntityCow>();
	
	// Watts being used per action
	public static final double	ENERGY_PER_MILK			= 150;
	
	// How much milk is stored?
	private LiquidTank			milkTank;
	private int					milkStored				= 0;
	private int					milkMaxStored			= 7 * LiquidContainerRegistry.BUCKET_VOLUME;
	private boolean				isMilking				= false;
	public static final int		PROCESS_TIME_REQUIRED	= 60;
	public int					processTicks			= 0;
	
	public tileEntityCowMilker()
	{
		super();
	}
	
	@Override
	public void updateEntity()
	{
		super.updateEntity();
		if (!worldObj.isRemote)
		{
			if (this.checkRedstone())
			{
				this.drainTo(ForgeDirection.DOWN);
				
				/* SCAN FOR COWS */
				if (this.ticks % 40 == 0)
				{
					scanForCows();
				}
				
				/* Milk Cows */
				if (this.ticks % 100 == 0 && this.milkStored <= this.milkMaxStored)
				{
					milkCows();
				}
				
				if (milkStored >= this.MilkPerBucket && inventory[2] != null && inventory[3] == null)
				{
					if (this.processTicks == 0)
					{
						this.processTicks = this.PROCESS_TIME_REQUIRED;
					}
					else if (this.processTicks > 0)
					{
						this.processTicks--;
						
						/**
						 * Process the item when the process timer is done.
						 */
						if (this.processTicks < 1)
						{
							if (inventory[2].stackSize > 1)
							{
								inventory[2].stackSize -= 1;
							}
							else if (inventory[2].stackSize == 1)
							{
								inventory[2] = null;
							}
							ItemStack bMilk = new ItemStack(Item.bucketMilk);
							inventory[3] = (bMilk);
							milkStored -= this.MilkPerBucket;
							this.processTicks = 0;
						}
					}
					else
					{
						this.processTicks = 0;
					}
					
				}
				if (milkStored >= milkMaxStored)
				{
					milkStored = milkMaxStored;
				}
			}
		}
	}
	
	/**
	 * Scans for cows for milking
	 */
	public void scanForCows()
	{
		AxisAlignedBB searchBox = AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord).expand(this.getMilkRange(), this.getMilkRange(), this.getMilkRange());
		this.CowList.clear();
		this.CowList.addAll(worldObj.getEntitiesWithinAABB(EntityCow.class, searchBox));
	}
	
	public void milkCows()
	{
		if (CowList.size() != 0 && this.getMilkStored() < this.getMaxMilk())
		{
			int vol = (10 * CowList.size());
			this.setMilkStored(vol, true);
			this.electricityStored -= ENERGY_PER_MILK;
		}
	}
	
	public int getMilkRange()
	{
		return 3;
	}
	
	public int getScanRange()
	{
		if (this.inventory[1] != null)
		{
			return (this.inventory[1].stackSize + 5);
		}
		return 3;
	}
	
	/**
	 * Drains the contents of the internal tank to a block bellow it
	 */
	public void drainTo(ForgeDirection dir)
	{
		TileEntity ent = worldObj.getBlockTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
		if (ent instanceof ITankContainer)
		{
			int filled = ((ITankContainer) ent).fill(dir.getOpposite(), Biotech.milkLiquid, true);
			if (filled > 0)
			{
				this.setMilkStored(filled, false);
			}
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound)
	{
		super.readFromNBT(tagCompound);
		this.milkStored = tagCompound.getInteger("milkStored");
		this.processTicks = tagCompound.getInteger("processTicks");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound)
	{
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("milkStored", (int) this.milkStored);
		tagCompound.setInteger("processTicks", (int) this.processTicks);
	}
	
	@Override
	public String getInvName()
	{
		return "Cow Milker";
	}
	
	@Override
	public void handlePacketData(INetworkManager network, int packetType, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		try
		{
			if (this.worldObj.isRemote)
			{
				this.milkStored = dataStream.readInt();
				this.processTicks = dataStream.readInt();
				this.facing = dataStream.readShort();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public Packet getDescriptionPacket()
	{
		return PacketHandler.getPacket(Biotech.CHANNEL, this, this.milkStored, this.processTicks, this.facing);
	}
	
	/**
	 * Sets the current volume of milk stored
	 * 
	 * @param amount
	 *            - volume sum
	 * @param add
	 *            - if true it will add the amount to the current sum
	 */
	public void setMilkStored(int amount, boolean add)
	{
		if (add)
		{
			this.milkStored += amount;
		}
		else
		{
			this.milkStored -= amount;
		}
	}
	
	public int getMilkStored()
	{
		return this.milkStored;
	}
	
	public int getMaxMilk()
	{
		return this.milkMaxStored;
	}
	
	/*
	 * @Override
	 * public ColorCode getColor()
	 * {
	 * return ColorCode.WHITE;
	 * }
	 * 
	 * @Override
	 * public void setColor(Object obj)
	 * {
	 * }
	 * 
	 * @Override
	 * public String getMeterReading(EntityPlayer user, ForgeDirection side)
	 * {
	 * return "Milk: " + this.milkStored + " Units";
	 * }
	 * 
	 * @Override
	 * public int getPressureOut(LiquidStack stack, ForgeDirection dir)
	 * {
	 * if (stack != null && this.color.isValidLiquid(stack))
	 * {
	 * return LiquidHandler.get(stack).getPressure();
	 * }
	 * return 0;
	 * }
	 * 
	 * @Override
	 * public boolean getCanPressureTo(LiquidStack stack, ForgeDirection dir)
	 * {
	 * return dir == ForgeDirection.DOWN.getOpposite() &&
	 * this.color.isValidLiquid(stack);
	 * }
	 */
	
	@Override
	public int fill(ForgeDirection from, LiquidStack resource, boolean doFill)
	{
		return 0;
	}
	
	@Override
	public int fill(int tankIndex, LiquidStack resource, boolean doFill)
	{
		return 0;
	}
	
	@Override
	public LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return drain(ForgeDirection.DOWN, maxDrain, doDrain);
	}
	
	@Override
	public LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain)
	{
		return milkTank.drain(maxDrain, doDrain);
	}
	
	@Override
	public ILiquidTank[] getTanks(ForgeDirection direction)
	{
		return null;
	}
	
	@Override
	public ILiquidTank getTank(ForgeDirection direction, LiquidStack type)
	{
		return null;
	}
}