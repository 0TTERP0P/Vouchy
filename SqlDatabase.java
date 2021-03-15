package Vouchy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class SqlDatabase {
	Connection con = getConnection();
	
	/*
	 * This method creates a connection to the SQL database specified in
	 * the reference class via the address username and password
	 */
	protected synchronized Connection getConnection() {
		try {
			return DriverManager.getConnection(  
					Ref.ADDRESS,Ref.USERNAME,Ref.PASSWORD);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/*
	 * Checks the sql database to see if the vouch already exists within it
	 * if it does it will return true otherwise, false.
	 */
	protected synchronized boolean checkVouch(User vUser_id, User author, Guild guild) throws SQLException {
		
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT (vUser_Id) FROM vouches "
								+ "WHERE vUser_Id = "+vUser_id.getId()+
								" AND User_ID = "+author.getId()+
								" AND Guild_ID = "+guild.getId()+
								" LIMIT 1");
		
		if(rs.next()) {
			rs.close();
			stmt.close();
			return true;
		}else {
			rs.close();
			stmt.close();
			return false;
		}

		
	}
	
	/*
	 * Inserts vouch into the vouches table of the database
	 */
	protected synchronized void insertVouch(User vUser_id,User author,Guild guild,String comments) throws SQLException{
		//Checking to see if the comment is null if not adding quotations
		//for later use in the query
		if(comments == null) 
			comments = "NULL";
		else
			comments = "\""+comments+"\"";
		
		Statement stmt = con.createStatement();
		LocalDateTime date = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		stmt.execute("INSERT INTO vouches VALUES("
				+ vUser_id.getId()+","+author.getId()+","+guild.getId()+",\""+date.format(formatter)
				+ "\","+comments+")");
		stmt.close();
	}
	
	/*
	 *  This will remove the authors vouch from the user provided
	 */
	protected synchronized void removeVouch(User vUser_id,User author,Guild guild) throws SQLException {
		Statement stmt = con.createStatement();
		stmt.execute("DELETE FROM vouches WHERE vuser_id = "+vUser_id.getId()+" AND user_id = "+author.getId()+" AND guild_id = "+guild.getId());
		stmt.close();
	}
	
	/*
	 * Will return the number of times a user was vouched within a guild
	 */
	protected synchronized int numVouch(User vUser_id,Guild guild) throws SQLException {
		int numVouches = 0;
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT COUNT(user_id) FROM vouches WHERE "
				+ "vuser_id = "+vUser_id.getId()+" AND guild_id = "+guild.getId());
		if(rs.next()) {
			if(Ref.DEVMODE) {
				System.out.println(rs.getInt(1 ));
			}
			numVouches = rs.getInt(1);
		}else {
			numVouches = -1;
		}
		rs.close();
		stmt.close();
		return numVouches;
	}
	
	protected synchronized List<String> getVouches(User vUser_id,Guild guild,int pageNumber) throws SQLException {
		ArrayList<String> results = new ArrayList<String>();
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("WITH cte AS ("
				+ "SELECT user_id,comments, ROW_NUMBER() OVER(ORDER BY user_id) AS row_num FROM vouches WHERE "
				+ "vuser_id = "+vUser_id.getId()+" AND guild_id = "+guild.getId()+")"
				+ "SELECT user_id,comments FROM cte WHERE row_num <= "+(pageNumber*10)+" AND row_num > "+((pageNumber-1)*10));
		while(rs.next()) {
			results.add(rs.getString("user_id")+":"+rs.getString("comments"));
		}
		rs.close();
		stmt.close();
		return results;
	}
	
	/*
	 * Checks whether or not the guild exists in the guilds table
	 * of the specified database
	 */
	protected synchronized boolean checkGuild(Guild guild) throws SQLException{
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE "
				+ "guild_id = "+guild.getId()+" LIMIT 1");
		if(rs.next()) {
			rs.close();
			stmt.close();
			return true;
		}else {
			rs.close();
			stmt.close();
			return false;
		}
	}
	
	/*
	 * Inserts the given guild into the guilds table of the database specified
	 * in the Ref class
	 */
	protected synchronized void insertGuild(Guild guild)throws SQLException{
		Statement stmt = con.createStatement();
		stmt.execute("INSERT INTO guilds VALUES("
				+ guild.getId()+","+guild.getOwnerId()+","+guild.getMemberCount()+")");
		stmt.close();
	}
	
	/*
	 * Removes the given guild from the guilds table of the database specified
	 * in the Ref class
	 */
	protected synchronized void removeGuild(Guild guild)throws SQLException{
		Statement stmt = con.createStatement();
		stmt.execute("DELETE FROM guilds WHERE guild_id = "+guild.getId());
		stmt.close();
	}
	
	/*
	 * Returns whether or not the given role can be found for the given guild
	 */
	protected synchronized boolean checkRole(Role role,Guild guild) throws SQLException {
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM roles WHERE "
				+ "role_id = "+role.getId()+" AND guild_id = "+guild.getId());
		if(rs.next()) {
			rs.close();
			stmt.close();
			return true;
		}else {
			rs.close();
			stmt.close();
			return false;
		}
	}
	
	
	/*
	 * Inserts the given role into the database
	 */
	protected synchronized void insertRole(Role role,Guild guild,String type,String numVouches)throws SQLException{
		Statement stmt = con.createStatement();
		type = "\""+type+"\"";
		stmt.execute("INSERT INTO roles VALUES("
				+ role.getId()+","+guild.getId()+","+type+","+(numVouches.contains("-1")?"NULL":numVouches)+")");
		stmt.close();
	}
	
	/*
	 * Deletes the role from the database
	 */
	protected synchronized void removeRole(Role role,Guild guild)throws SQLException{
		Statement stmt = con.createStatement();
		stmt.execute("DELETE FROM roles WHERE role_id = "+role.getId()+" AND "+"guild_id = "+guild.getId());
		stmt.close();
	}
	
	/*
	 * Returns all of the roles the guild has in the database using the format
	 * role_id role_type vouches_needed
	 */
	protected synchronized List<String> getRoles(Guild guild)throws SQLException{
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM roles WHERE guild_id = "+guild.getId()+" ORDER BY role_type ");
		ArrayList<String> results = new ArrayList<String>();
		while(rs.next()) {
			results.add(guild.getRoleById(rs.getString("role_id")).getAsMention()+" "+rs.getString("role_type")+" "+rs.getInt("vouches_needed"));
		}
		rs.close();
		stmt.close();
		return results;
	}
	
	/*
	 * Returns all of the roles in the guild in a list of role objects
	 */
	protected synchronized List<Role> getRoles(Guild guild, String type)throws SQLException{
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT role_id FROM roles WHERE guild_id = "+guild.getId()+" AND role_type = "+"\""+type+"\"");
		ArrayList<Role> results = new ArrayList<Role>();
		while(rs.next()) {
			results.add(guild.getRoleById(rs.getString("role_id")));
		}
		rs.close();
		stmt.close();
		return results;
	}
	/*
	 * Returns all of the roles of a certain type the guild has in the database using the format
	 * role_id guild_id vouches_needed
	 */
	protected synchronized List<String> getRolesString(Guild guild, String type)throws SQLException{
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM roles WHERE guild_id = "+guild.getId()+" AND role_type = "+"\""+type+"\"");
		ArrayList<String> results = new ArrayList<String>();
		while(rs.next()) {
			results.add(rs.getString("role_id")+" "+rs.getString("guild_id")+" "+rs.getString("vouches_needed"));
		}
		rs.close();
		stmt.close();
		return results;
	}
	
}
