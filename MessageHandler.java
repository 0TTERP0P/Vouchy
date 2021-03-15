package Vouchy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

final class MessageHandler implements Runnable {
	private MessageReceivedEvent evt;
	private String threadName;
	private Thread thread;
	private SqlDatabase db = Main.db;
	
	
	protected MessageHandler(MessageReceivedEvent event, String threadID) {
		evt = event;
		threadName = threadID;
	}

	@Override
	public void run() {
		
		
		Message msg = evt.getMessage();
		MessageChannel channel = msg.getChannel();
		String rawMsg = msg.getContentRaw();
		if (Ref.DEVMODE) {
			System.out.println("Thread running: " + threadName + "\n" + rawMsg);
		}

		// Checking to make sure the message is a command to be used by this bot
		if (rawMsg.startsWith(Ref.PREFIX)) {
			List<String> parsedCommand = Ref.parse(rawMsg, " ");
			String command = parsedCommand.get(0);
			//These booleans will dictate wether the user who sent the message is an admin
			//and if the user is allowed to use the bot at all
			boolean isAdmin = false;
			boolean hasRequired = true;
			try {
				List<Role> adminRoles = db.getRoles(msg.getGuild(), "ADMIN");
				List<Role> requiredRoles = db.getRoles(msg.getGuild(), "REQUIRED");
				List<Role> authorRoles = msg.getMember().getRoles();
				//This ensures the owner will be able to use the bot no matter what the
				//role requirements are
				if (msg.getMember().isOwner())
					isAdmin = true;
				else {
					//This will check the lists above (adminRoles,requiredRoles) to check wether
					//the authorRoles contains the adminRoles or a required role
					for (Role r : authorRoles) {
						if (adminRoles.contains(r)) {
							isAdmin = true;
							break;
						}
					}
					if (!requiredRoles.isEmpty()) {

						for (Role r : requiredRoles) {
							if (!authorRoles.contains(r)) {
								hasRequired = false;
							}
						}
					}
				}

			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			if (Ref.DEVMODE)
				System.out.println(command);


			if (hasRequired) {
/**************************************************************************************************************************
 * The beginning of the command section
 ************************************************************************************************************************/
//Help/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				if (command.equalsIgnoreCase(Ref.PREFIX + "help") || command.equalsIgnoreCase(Ref.PREFIX + "?")
						|| command.equalsIgnoreCase(Ref.PREFIX + "h")) {
					sendMsg("List of commands:\n" + Ref.PREFIX
							+ "vouch @user (comments) - this command will allow the author to vouch for the mentioned user\n"
							+ Ref.PREFIX + "unvouch @user - Will remove the authors vouch from the user\n" + Ref.PREFIX
							+ "checkvouch @user - Will display the number of vouches a user has\n"
							+ Ref.PREFIX+"checkcomments @user <page number> - Will show you the comments people have left for the user\n"
							+ Ref.PREFIX+"addrole @role <type> (optional)<Number of vouches needed>- admin comand that will add a role condition based on the type\n"
									+ "	The ADMIN type will give that role access to admin commands\n"
									+ "	The REQUIRED type will make it so you must have that role to use ANY command\n"
									+ "	The ACHIEVEMENT type will give the player the provided role when they reach the number of vouches specified\n"
							+ Ref.PREFIX+"removerole @role - admin command that will remove the given role condition\n"
							+ Ref.PREFIX+"listroles - admin command that will show all role conditions", channel);

//Vouch//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "vouch")
						|| command.equalsIgnoreCase(Ref.PREFIX + "v")) {

					try {
						User user = msg.getMentionedUsers().get(0);
						Member member = msg.getMentionedMembers().get(0);
						String parsedComment = Ref.connect(parsedCommand, 2);
						// This if statement will go through the following checks
						// User is not self, User is not a bot, User has not already been vouched by
						// author
						// If it succeeds the user will be vouched by the author
						if (user.equals(msg.getAuthor())) {
							sendMsg("You can't vouch for yourself!", channel);
						} else if (db.checkVouch(user, msg.getAuthor(), msg.getGuild())) {
							sendMsg("You've already vouched for this person!", channel);
						} else if (user.isBot()) {
							sendMsg("You cant vouch for a bot!", channel);
						} else if (parsedComment.length() > 100) {
							sendMsg("Could not vouch comment must be less or equal too 100 characters.",channel);
						} else {
							db.insertVouch(user, msg.getAuthor(), msg.getGuild(), parsedComment);
							if (parsedComment.isEmpty() || parsedComment == "NULL")
								sendMsg(msg.getAuthor().getAsMention() + " has vouched for " + user.getAsMention()
										+ "!", channel);
							else {
								sendMsg(msg.getAuthor().getAsMention() + " has vouched for " + user.getAsMention() + "!"
										+ "\nand left \"" + parsedComment + "\" as a comment.", channel);
							}
							updateRoles(member, msg.getGuild(), channel);
						}

					} catch (IndexOutOfBoundsException e) {
						sendMsg("Could not find user, make sure you mention them using the @ symbol.", channel);
					} catch (SQLException e) {
						sendMsg("SQL exception contact support with a bug report!\nID:1", channel);
						e.printStackTrace();
					}
//Unvouch///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "unvouch")
						|| command.equalsIgnoreCase(Ref.PREFIX + "uv")) {
					try {
						User user = msg.getMentionedUsers().get(0);
						Member member = msg.getMentionedMembers().get(0);
						// Checking that the user has been vouched by the author previously
						if (db.checkVouch(user, msg.getAuthor(), msg.getGuild())) {
							db.removeVouch(user, msg.getAuthor(), msg.getGuild());
							sendMsg(msg.getAuthor().getAsMention() + "\'s vouch has been removed from "
									+ user.getAsMention() + ".", channel);
							updateRoles(member, msg.getGuild(), channel);
						} else {
							sendMsg("You have not vouched for this user!", channel);
						}

					} catch (IndexOutOfBoundsException e) {
						sendMsg("Could not find user, make sure you mention them using the @ symbol.", channel);
					} catch (SQLException e) {
						sendMsg("SQL exception contact support with a bug report!\nID:2", channel);
						e.printStackTrace();
					}
//Checkvouch/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "checkvouch")
						|| command.equalsIgnoreCase(Ref.PREFIX + "checkvouches")
						|| command.equalsIgnoreCase(Ref.PREFIX + "cv")
						|| command.equalsIgnoreCase(Ref.PREFIX + "cvs")) {
					try {
						User user = msg.getMentionedUsers().get(0);
						Member member = msg.getMentionedMembers().get(0);
						int numVouches = db.numVouch(user, msg.getGuild());
						//Ensuring the user actually appears in the database if not letting
						//the author know the user has no vouches at all
						if (numVouches < 0) {
							sendMsg("Could not find any vouches for that user", channel);
						} else {
							sendMsg(user.getAsMention() + " has " + numVouches + " vouches", channel);
						}
						updateRoles(member, msg.getGuild(), channel);

					} catch (IndexOutOfBoundsException e) {
						sendMsg("Could not find user, make sure you mention them using the @ symbol.", channel);
					} catch (SQLException e) {
						sendMsg("SQL exception contact support with a bug report!\nID:3", channel);
						e.printStackTrace();
					}
//CheckComments//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "checkcomments")
						|| command.equalsIgnoreCase(Ref.PREFIX + "cc")) {
					try {
						User user = msg.getMentionedUsers().get(0);
						int page = Integer.parseInt(parsedCommand.get(2));
						List<String> vouches = db.getVouches(user, msg.getGuild(),page);
						//Checking to see if there are any comments for the user
						if(vouches.isEmpty()) {
							sendMsg("No comments left for this user",channel);
						}else {
							String comments = "";
							//Looping through the list of comments and connecting removing the user's id from them
							for(String str:vouches) {
								List<String> parsed = Ref.parse(str, ":");
								comments+= msg.getGuild().getMemberById(parsed.get(0)).getUser().getName()+"\n\""
										+Ref.connect(parsed, 1)+"\"\n";
							}
							sendMsg("Here is page "+page+" of comments for "+user.getAsMention()+"\n"+comments,channel);
						}

					} catch (IndexOutOfBoundsException e) {
						sendMsg("Could not find user, make sure you mention them using the @ symbol.", channel);
					} catch (SQLException e) {
						sendMsg("SQL exception contact support with a bug report!\nID:4", channel);
						e.printStackTrace();
					}
//removerole//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "removerole")
						|| command.equalsIgnoreCase(Ref.PREFIX + "rr")) {
					if (isAdmin) {
						try {
							Role role = msg.getMentionedRoles().get(0);
							//Ensuring the role exists in the database
							if (db.checkRole(role, msg.getGuild())) {
								db.removeRole(role, msg.getGuild());
								sendMsg("Removed the role " + role.getName(), channel);
							} else {
								sendMsg("This role does not exist!", channel);
							}
						} catch (IndexOutOfBoundsException e) {
							sendMsg("Could not find that role make sure you mention it via the @ symbol", channel);
						} catch (SQLException e) {
							sendMsg("SQL exception contact support with a bug report!\nID:5", channel);
							e.printStackTrace();
						}
					} else {
						sendMsg("You need the an Administrative role to use this command!", channel);
					}
//listroles////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "listroles")
						|| command.equalsIgnoreCase(Ref.PREFIX + "lr")) {
					if (isAdmin) {
						try {
							String message = "Here are all the roles, their types, and the vouches needed:\n";
							List<String> roles = db.getRoles(msg.getGuild());
							//Putting all the roles into a format the reader can understand
							for (String s : roles) {
								message += s + "\n";
							}
							sendMsg(message, channel);
						} catch (SQLException e) {
							sendMsg("SQL exception contact support with a bug report!\nID:6", channel);
							e.printStackTrace();
						}
					} else {
						sendMsg("You need the an Administrative role to use this command!", channel);
					}
//addrole//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				} else if (command.equalsIgnoreCase(Ref.PREFIX + "addrole")
						|| command.equalsIgnoreCase(Ref.PREFIX + "ar")) {
					if (isAdmin) {
						try {
							Role role = msg.getMentionedRoles().get(0);
							//Checking whether the role is already in the database
							if (db.checkRole(role, msg.getGuild())) {
								sendMsg("This role already exists!", channel);
							} else {
								//Getting the role type and inserting it into the database
								//Admin or required
								if (parsedCommand.get(2).equalsIgnoreCase("admin")
										|| parsedCommand.get(2).equalsIgnoreCase("required")) {
									db.insertRole(role, msg.getGuild(), parsedCommand.get(2).toUpperCase(), "-1");
									sendMsg(role.getName() + " has been set as a " + parsedCommand.get(2) + " role",
											channel);
								//Achievement
								} else if (parsedCommand.get(2).equalsIgnoreCase("achievement")) {
									db.insertRole(role, msg.getGuild(), parsedCommand.get(2).toUpperCase(),
											parsedCommand.get(3));
									sendMsg(role.getName() + " has been set as a " + parsedCommand.get(2) + " role",
											channel);
								} else
									sendMsg("Make sure the type is one of the following:\nAdmin, Required, Achievement\n and that there is only one space following the mention",
											channel);
							}

						} catch (SQLException e) {
							sendMsg("SQL exception contact support with a bug report!\nID:7", channel);
							e.printStackTrace();
						}
					} else {
						sendMsg("You need the an Administrative role to use this command!", channel);
					}

				}
/***************************************************************************************************************************************************************
 * 				End of commands
 ****************************************************************************************************************************************************************/

			} else {
				sendMsg("You do not have the required role!", channel);
			}
		}
	}
	
	
/*
 * This method will update the roles of the given member within the given guild and send a confirmation
 * message to the given channel.
 */
	private void updateRoles(Member user, Guild guild, MessageChannel channel) throws SQLException {
		int numVouches = db.numVouch(user.getUser(), guild);
		List<String> roles = db.getRolesString(guild, "ACHIEVEMENT");
		//Ensuring there are achievement roles to check
		if (!roles.isEmpty()) {
			List<Role> userRoles = user.getRoles();
			//Looping through all the provided achievment roles
			//and checking to makse sure the user has the number of vouches
			//to have them then either adding or removing the role depending on 
			//the users vouch count and the roles requirements
			for (String s : roles) {
				List<String> parsed = Ref.parse(s, " ");
				Role r = guild.getRoleById(parsed.get(0));
				int neededVouches = Integer.parseInt(parsed.get(2));
				if (numVouches >= neededVouches && userRoles.isEmpty()
						|| numVouches >= neededVouches && !userRoles.contains(r)) {
					guild.addRoleToMember(user, r).queue();
					sendMsg(user.getAsMention() + " has been given the role " + r.getName() + " for achieving "
							+ neededVouches + " vouches!", channel);
				} else if (numVouches < neededVouches && userRoles.contains(r)) {
					guild.removeRoleFromMember(user, r).queue();
					sendMsg(user.getAsMention() + " has lost the " + r.getName() + " role.", channel);
				} else if (Ref.DEVMODE)
					System.out.println(numVouches + " " + neededVouches);

			}
		}
	}

	protected void start() {
		if (thread == null) {
			thread = new Thread(this, threadName);
			thread.start();
		}
		if (Ref.DEVMODE) {
			System.out.println("Thread starting: " + threadName);
		}
	}

	protected static synchronized void sendMsg(String message, MessageChannel channel) {
		channel.sendMessage(message).queue();
	}
}
