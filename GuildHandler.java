package Vouchy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.dv8tion.jda.api.entities.Guild;

public class GuildHandler implements Runnable{
	private Thread thread;
	private String threadName;
	private String type;
	private Guild guild;
	public GuildHandler(String threadID,String type, Guild g) {
		threadName = threadID;
		this.type = type;
		guild = g;
	}
	
	@Override
	public void run() {
		
		//This check will decide whether to add new info or remove info from the database
		if(type.equalsIgnoreCase("join")) {
			try {
				if(Main.db.checkGuild(guild)) {
					if(Ref.DEVMODE) {
						System.out.println("Guild already exists.");
					}
				}else {
					Main.db.insertGuild(guild);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		
		}else if(type.equalsIgnoreCase("leave")) {
			try {
				Main.db.removeGuild(guild);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//Starting a new thread for this object
	protected void start() {
		if(thread==null) {
			thread = new Thread(this,threadName);
			thread.start();
		}
		if(Ref.DEVMODE) {
			System.out.println("Thread starting: "+threadName);
		}
	}
}
