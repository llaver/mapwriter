package mapwriter.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import mapwriter.Mw;
import mapwriter.config.ConfigurationHandler;
import mapwriter.map.mapmode.MapMode;
import mapwriter.util.Reference;
import mapwriter.util.Render;
import mapwriter.util.Utils;

public class Entities
{

	class Entity
	{

		double x, y, z, heading;
		int alphaPercent;

		public String entityType;

		static final int borderColour = 0xff000000;
		static final int colour = 0xff00ffff;

		public Entity(double x, double y, double z, double heading, String entityType)
		{
			this.set(x, y, z, heading, entityType);
		}

		public void set(double x, double y, double z, double heading, String entityType)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.heading = heading;
			this.alphaPercent = 100;
			this.entityType = Utils.mungeStringForConfig(entityType);
		}

		public void draw(MapMode mapMode, MapView mapView)
		{
			if (mapView.isBlockWithinView(this.x, this.z, mapMode.config.circular))
			{
				Point.Double p = mapMode.blockXZtoScreenXY(mapView, this.x, this.z);

				//TODO Draw something to represent that entity

				// draw a coloured arrow centered on the calculated (x, y)
				Render.setColourWithAlphaPercent(borderColour, this.alphaPercent);
				Render.drawArrow(p.x, p.y, this.heading, mapMode.config.trailMarkerSize);
				Render.setColourWithAlphaPercent(colour, this.alphaPercent);
				Render.drawArrow(p.x, p.y, this.heading, mapMode.config.trailMarkerSize - 1.0);
			}
		}
	}

	private Mw mw;
	public LinkedList<Entity> entityList = new LinkedList<Entity>();
	public int maxLength = 50;
	public String name;
	public boolean enabled;
	public long lastMarkerTime = 0;
	public long intervalMillis = 5000;

	public List<String> groupList = new ArrayList<String>();
	public List<Entity> visibleEntityList = new ArrayList<Entity>();

	private String visibleEntityTypes = "all";

	public Entities(Mw mw, String name)
	{
		this.mw = mw;
		this.name = name;
		//TODO update and add settings/configs
		this.enabled = ConfigurationHandler.configuration.getBoolean( "EntitiesEnabled", Reference.catOptions, true, "");
		this.maxLength = ConfigurationHandler.configuration.getInt(this.name + "TrailMaxLength", Reference.catOptions, this.maxLength, 1, 200, "");
		this.intervalMillis = ConfigurationHandler.configuration.getInt(this.name + "TrailMarkerIntervalMillis", Reference.catOptions, (int) this.intervalMillis, 100, 360000, "");
	}

	public void close()
	{
		// this.mw.config.setBoolean(Mw.catOptions, this.name + "TrailEnabled",
		// this.enabled);
		// this.mw.config.setInt(Mw.catOptions, this.name + "TrailMaxLength",
		// this.maxLength);
		// this.mw.config.setInt(Mw.catOptions, this.name +
		// "TrailMarkerIntervalMillis", (int) this.intervalMillis);
		this.entityList.clear();
	}

	// for other types of trails will need to extend Trail and override this
	// method
	public void onTick()
	{
		long time = System.currentTimeMillis();
		if ((time - this.lastMarkerTime) > this.intervalMillis)
		{
			this.lastMarkerTime = time;
			//TODO update with proper values
			this.addEntity(this.mw.playerX, this.mw.playerY, this.mw.playerZ, this.mw.playerHeading, null);
		}
	}

	public void addEntity(double x, double y, double z, double heading, String groupName)
	{
		this.entityList.add(new Entity(x, y, z, heading, groupName));
		// remove elements from beginning of list until the list has at most
		// maxTrailLength elements
		while (this.entityList.size() > this.maxLength)
		{
			this.entityList.poll();
		}
		int i = this.maxLength - this.entityList.size();
		for (Entity marker : this.entityList)
		{
			marker.alphaPercent = (i * 100) / this.maxLength;
			i++;
		}
	}

	public void update()
	{
		this.entityList.clear();
		fetchPlayers();
		fetchMobs();
		this.visibleEntityList.clear();
		for (Entity entity : this.entityList)
		{
			if (this.visibleEntityTypes.contains(entity.entityType) || this.visibleEntityTypes.contains("all"))
			{
				this.visibleEntityList.add(entity);
			}
		}
	}

	public void draw(MapMode mapMode, MapView mapView)
	{
		for (Entity entity : this.entityList)
		{
			entity.draw(mapMode, mapView);
		}
	}

	public void fetchMobs() {
		double max = (Math.pow(2,minimap.lZoom) * 16 - 0);
		java.util.List entities = this.mw.mc.theWorld.getLoadedEntityList();
		for(int j = 0; j < entities.size(); j++) {
			Entity entity = (Entity)entities.get(j);
			try {
				if(  (showHostiles && isHostile(entity)) || (showPlayers && isPlayer(entity)) || (showNeutrals && isNeutral(entity)) ) {
					int wayX = this.xCoord() - (int)(entity.posX);
					int wayZ = this.zCoord() - (int)(entity.posZ);
					int wayY = this.yCoord() - (int)(entity.posY);
					// sqrt version
					//double hypot = Math.sqrt((wayX*wayX)+(wayZ*wayZ)+(wayY*wayY));
					//hypot = hypot/(Math.pow(2,minimap.lZoom)/2);
					//if (hypot < 31.0D) {

					// no sqrt version
					double hypot = ((wayX*wayX)+(wayZ*wayZ)+(wayY*wayY));
					hypot = hypot/((Math.pow(2,minimap.lZoom)/2)*(Math.pow(2,minimap.lZoom)/2));
					if (hypot < 961.0D /*31.0D squared - saves on sqrt ops*/) {
						//System.out.println("player: " + (int)this.game.thePlayer.posY + " mob: " + (int)(entity.posY));

						Contact contact;
						if (isPlayer(entity))
							contact = handleMPplayer(entity);
						else
							contact = new Contact(entity, /*(int)*/(entity.posX), /*(int)*/(entity.posZ), (int)(entity.posY), getContactType(entity));
						contact.angle = (float)Math.toDegrees(Math.atan2(wayX, wayZ));
						// pow2,0 is 1.  /2 is 1/2.  so if hypot max 16/.5 < 31.  pow2,1 is 2. /2 is 1.  hypot max 32/1 < 31.  pow2,2 is 4. /2 is 2.  hypot max 64/2 < 31
						contact.distance = Math.sqrt((wayX*wayX)+(wayZ*wayZ))/(Math.pow(2,minimap.lZoom)/2);
						double adjustedDiff = max - Math.max((Math.abs(wayY) - 0), 0);
						contact.brightness = (float)Math.max(adjustedDiff / max, 0);
						contact.brightness *= contact.brightness;
						contacts.add(contact);

						//contacts.add(new Contact((int)(entity.posX), (int)(entity.posZ), (int)(entity.posY), getContactType(entity)));
					} // end if valid contact
				} // end if should be displayed
			} catch (Exception classNotFoundException) {
				this.error = "class not found";
			}
		} // end for loop contacts
		Collections.sort(contacts, new java.util.Comparator<Contact>() {
			public int compare(Contact contact1, Contact contact2) {
				return contact1.y - contact2.y;
			}
		});
		this.lastX = this.xCoordDouble();
		this.lastZ = this.zCoordDouble();
		this.lastY = this.yCoord();
		this.lastZoom = this.minimap.lZoom;
	}

	public Entity fetchPlayers(Entity entity) {
		String playerName = scrubCodes(((EntityOtherPlayerMP)entity).username);
		String skinURL = ((EntityOtherPlayerMP)entity).skinUrl;
		Contact mpContact = new Contact(entity, /*(int)*/(entity.posX), /*(int)*/(entity.posZ), (int)(entity.posY), getContactType(entity));
		mpContact.setName(playerName);
		Integer ref = mpContacts.get(playerName+0); // don't load if already done
		//System.out.println("***********CHECKING " + ref);
		if (ref == null) { // if we haven't encountered player yet, try to get MP skin
			ThreadDownloadImageData imageData = this.renderEngine.obtainImageData(skinURL, new ImageBufferDownload());
			if (imageData == null || imageData.image == null) { // failed to get
				BufferedImage skinImage = loadSkin(playerName); // try to load icon saved to disk
				if (skinImage != null) { // if there is one, 128it and use
					//skinImage = intoSquare(skinImage); // actally square it.  actually don't bother should all be 8x8
					BufferedImage skinImageSmall = fillOutline(intoSquare(skinImage)); // add space around edge and make ready for filtering
					BufferedImage skinImageLarge = fillOutline(intoSquare(scaleImage(skinImage, 2)));
					int imageRef = this.tex(skinImageSmall);
					mpContacts.put(playerName+0, imageRef);
					//System.out.println("Loading " + playerName + " from disk: NEW REF " + imageRef);
					imageRef = this.tex(skinImageLarge);
					mpContacts.put(playerName+1, imageRef);
				}
				else { // else default
					mpContacts.put(playerName, -1); // so make image ref for this player the standard player icon.  Try for hidef (if it's the same as lodef, well, lodef will show)
					//System.out.println("***********DEFAULTING " + imageRef[PLAYER][1]);
				}
			}
			else { // we got a downloaded image
				BufferedImage skinImage = imageData.image;
				//skinImage = into128(addImages(loadImage(skinImage, 8, 8, 8, 8), loadImage(skinImage, 40, 8, 8, 8), 0, 0));
				skinImage = addImages(loadImage(skinImage, 8, 8, 8, 8), loadImage(skinImage, 40, 8, 8, 8), 0, 0, 8, 8);
				saveSkin(skinImage, playerName); // save for future use when skin server is down
				BufferedImage skinImageSmall = fillOutline(intoSquare(skinImage)); // add space around edge and make ready for filtering
				BufferedImage skinImageLarge = fillOutline(intoSquare(scaleImage(skinImage, 2)));
				int imageRef = this.tex(skinImageSmall);
				mpContacts.put(playerName+0, imageRef);
				//System.out.println("***********NEW REF " + imageRef);
				imageRef = this.tex(skinImageLarge);
				mpContacts.put(playerName+1, imageRef);
			}
			if (imageData != null)
				this.renderEngine.releaseImageData(skinURL); // if it was not null, the reference was incremented.  Decrement it!
		}
		return mpContact;

	}

}
