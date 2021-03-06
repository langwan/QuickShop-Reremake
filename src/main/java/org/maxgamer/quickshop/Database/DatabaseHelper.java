package org.maxgamer.quickshop.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.Util;

public class DatabaseHelper {
	public static void setup(Database db) throws SQLException {
		connectCheck();
		if (!db.hasTable(QuickShop.instance.dbPrefix+"shops")) {
			createShopsTable(db);
		}
		if (!db.hasTable(QuickShop.instance.dbPrefix+"messages")) {
			createMessagesTable(db);
		}
		checkColumns(db);
	}

	/**
	 * Verifies that all required columns exist.
	 */
	public static void checkColumns(Database db) {
		connectCheck();
		PreparedStatement ps = null;
		try {
			// V3.4.2
			ps = db.getConnection().prepareStatement("ALTER TABLE "+QuickShop.instance.dbPrefix+"shops MODIFY COLUMN price double(32,2) NOT NULL AFTER owner");
			ps.execute();
			ps.close();
		} catch (SQLException e) {
		}
		try {
			// V3.4.3
			ps = db.getConnection().prepareStatement("ALTER TABLE "+QuickShop.instance.dbPrefix+"messages MODIFY COLUMN time BIGINT(32) NOT NULL AFTER message");
			ps.execute();
			ps.close();
		} catch (SQLException e) {
		}
	}

	/**
	 * Creates the database table 'shops'.
	 * 
	 * @throws SQLException
	 *             If the connection is invalid.
	 */
	public static void createShopsTable(Database db) throws SQLException {
		connectCheck();
		Statement st = db.getConnection().createStatement();
		String createTable = null;
		if(QuickShop.instance.getConfig().getBoolean("database.use-varchar")) {
			createTable = "CREATE TABLE "+QuickShop.instance.dbPrefix+"shops (owner  VARCHAR(255) NOT NULL, price  double(32, 2) NOT NULL, itemConfig TEXT CHARSET utf8 NOT NULL, x  INTEGER(32) NOT NULL, y  INTEGER(32) NOT NULL, z  INTEGER(32) NOT NULL, world VARCHAR(32) NOT NULL, unlimited  boolean, type  boolean, PRIMARY KEY (x, y, z, world) );";
		}else {
			createTable = "CREATE TABLE "+QuickShop.instance.dbPrefix+"shops (owner  TEXT(32) NOT NULL, price  double(32, 2) NOT NULL, itemConfig TEXT CHARSET utf8 NOT NULL, x  INTEGER(32) NOT NULL, y  INTEGER(32) NOT NULL, z  INTEGER(32) NOT NULL, world VARCHAR(32) NOT NULL, unlimited  boolean, type  boolean, PRIMARY KEY (x, y, z, world) );";
		}
		st.execute(createTable);
	}

	/**
	 * Creates the database table 'messages'
	 * 
	 * @throws SQLException
	 *             If the connection is invalid
	 */
	public static void createMessagesTable(Database db) throws SQLException {
		connectCheck();
		Statement st = db.getConnection().createStatement();
		String createTable = null;
		if(QuickShop.instance.getConfig().getBoolean("database.use-varchar")) {
			createTable = "CREATE TABLE "+QuickShop.instance.dbPrefix+"messages (owner  VARCHAR(255) NOT NULL, message  TEXT(25) NOT NULL, time  BIGINT(32) NOT NULL );";
		}else {
			createTable = "CREATE TABLE "+QuickShop.instance.dbPrefix+"messages (owner  TEXT(32) NOT NULL, message  TEXT(25) NOT NULL, time  BIGINT(32) NOT NULL );";
		}
		
		st.execute(createTable);
	}
	
	public static ResultSet selectAllShops(Database db) throws SQLException {
		connectCheck();
		PreparedStatement ps =  db.getConnection().prepareStatement("SELECT * FROM "+QuickShop.instance.dbPrefix+"shops");
		return ps.executeQuery();
		
	}
	public static ResultSet selectAllMessages(Database db) throws SQLException {
		connectCheck();
		PreparedStatement ps =  db.getConnection().prepareStatement("SELECT * FROM "+QuickShop.instance.dbPrefix+"messages");
		return ps.executeQuery();
		
	}

	public static void removeShop(Database db, int x, int y, int z, String worldName) throws SQLException {
		connectCheck();
		db.getConnection().createStatement()
				.executeUpdate("DELETE FROM " + QuickShop.instance.dbPrefix + "shops WHERE x = " + x + " AND y = " + y
						+ " AND z = " + z + " AND world = \"" + worldName + "\""
						+ (db.getCore() instanceof MySQLCore ? " LIMIT 1" : ""));
	}
	public static void updateOwner2UUID(String ownerUUID, int x, int y, int z, String worldName) throws SQLException {
		connectCheck();
		QuickShop.instance.getDB().getConnection().createStatement()
		.executeUpdate("UPDATE "+QuickShop.instance.dbPrefix+"shops SET owner = \"" + ownerUUID.toString()
		+ "\" WHERE x = " + x + " AND y = " + y + " AND z = " + z
		+ " AND world = \"" + worldName + "\" LIMIT 1");
		
	}

	public static void updateShop(Database db, String owner, ItemStack item, int unlimited, int shopType,
			double price, int x, int y, int z, String world) {
		connectCheck();
		String q = "UPDATE "+QuickShop.instance.dbPrefix+"shops SET owner = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
		db.execute(q, owner, Util.serialize(item), unlimited, shopType, price, x, y, z, world);
	}
	
	public static void createShop(String owner, double price, ItemStack item, int unlimited, int shopType, String world, int x, int y, int z) {
		connectCheck();
		String q = "INSERT INTO "+QuickShop.instance.dbPrefix+"shops (owner, price, itemConfig, x, y, z, world, unlimited, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		QuickShop.instance.getDB().execute(q, owner, price, Util.serialize(item), x, y, z, world, unlimited, shopType);
	}
	public static void sendMessage(UUID player,String message,long time) {
		String q = "INSERT INTO "+QuickShop.instance.dbPrefix+"messages (owner, message, time) VALUES (?, ?, ?)";
		QuickShop.instance.getDB().execute(q, player.toString(), message, System.currentTimeMillis());
	}
	
	public static void cleanMessage(long weekAgo) {
		QuickShop.instance.getDB().execute("DELETE FROM "+QuickShop.instance.dbPrefix+"messages WHERE time < ?", weekAgo);
	}
	
	public static void connectCheck(){
		if(!QuickShop.instance.getConfig().getBoolean("database.reconnect")) {
			return;
		}
		try {
			if(QuickShop.instance.getDB().getConnection().isClosed())
			QuickShop.instance.getLogger().severe("Database connection lost, Reconnecting...");
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			if(QuickShop.instance.getDB().getConnection().isReadOnly()) {
			QuickShop.instance.getLogger().severe("Database is read-only, QuickShop can't write in data!");
			return;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			try {
				if (!QuickShop.instance.getDB().getConnection().isValid(10)) {
					boolean result = QuickShop.instance.setupDatabase(); // Reconnect
					if(result) {
						//Failed reconnect.
						QuickShop.instance.getLogger().severe("Failed to reconnect.");
					}else {
						QuickShop.instance.getLogger().severe("Reconnected.");
						connectCheck(); //Recall to check
					}
				}
			} catch (AbstractMethodError e) {
				// You don't need to validate this core.
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
}