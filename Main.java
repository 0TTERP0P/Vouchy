package Vouchy;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

final class Main extends ListenerAdapter{
	protected static SqlDatabase db = new SqlDatabase();
	public static void main(String[] args) {
		//Initial setup
		try {
			
			JDA jda = JDABuilder.createDefault(Ref.TOKEN).build();
			jda.addEventListener(new Main());
			if(Ref.DEVMODE)
				System.out.println("JDA Successfully created");
		} catch (LoginException e) {
			if(Ref.DEVMODE) {
				System.out.println("Failed to connect.");
				e.printStackTrace();
			}
		}
		
	}
	
	

	
	
	@Override
	/*
	 * Creates a new thread for the MessageHandler class to run on
	 * as well as passing the event to class to be processed
	 */
	public void onMessageReceived(MessageReceivedEvent evt) {
		if(!evt.getAuthor().isBot()) {
			MessageHandler mh = new MessageHandler(evt,evt.getMessageId());
			mh.start();
			
		}
	}
	

	/*
	 * Creates a new instance of the GuildHandler Class to update the database
	 * with the new guilds info
	 */
	public void onGuildJoin(GuildJoinEvent event) {
		GuildHandler gh = new GuildHandler(event.getGuild().getId()+"join","join",event.getGuild());
		gh.start();
	}
	
	
	/*
	 * Creates a new Instance of the GuildHandler Class that will remove the guilds
	 * info from the database
	 */
	public void onGuildLeave(GuildLeaveEvent event) {
		GuildHandler gh = new GuildHandler(event.getGuild().getId()+"leave","leave",event.getGuild());
		gh.start();
	}
	
	
}
