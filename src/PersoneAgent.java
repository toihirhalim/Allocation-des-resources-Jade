import java.io.IOException;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class PersoneAgent extends Agent {

	private boolean status;
	private AID[] restaurants;
	private AID[] people;
	private long time;
	private int indexRandomRestaurant;
	private Random rand = new Random();
	private int state;
	private int nombreAppel = 0;
	private boolean reservedRestaurant = false;

	protected void setup() {
		System.out.println("Persone : "+getAID().getLocalName()+" is ready.");
		time = rand.nextInt(15000) + 5000;
		state = 0;
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("people");
		sd.setName("people");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new TickerBehaviour(this, time) {
			protected void onTick() {
				if(!reservedRestaurant) {
					state = 1;
					System.out.println("\n\nPersone : "+getAID().getLocalName()+" Trying to reserve a place.");
	
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("reservation-restaurant");
					template.addServices(sd);
					
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.print("Persone : "+getAID().getLocalName()+" found the following restaurant : [ ");
	
						restaurants = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							restaurants[i] = result[i].getName();
							System.out.print(restaurants[i].getLocalName() + ", ");
						}
						System.out.println(" ]");
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
				
					myAgent.addBehaviour(new CallForReservation());
				}

			}
		} );

		addBehaviour(new RecieveMessageFromPeople());
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("Persone : "+getAID().getLocalName()+" terminating with "+ nombreAppel +" Call(s) .\n");
		
		
	}
	
	private class CallForReservation extends Behaviour {

		private MessageTemplate mt;
		private int step = 0;

		public void action() {
			
			switch (step) {
			case 0:

				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				indexRandomRestaurant = rand.nextInt(restaurants.length);
				System.out.println("Personne : " + getAID().getLocalName() + " chose the restaurant : " + restaurants[indexRandomRestaurant].getLocalName());
				cfp.addReceiver(restaurants[indexRandomRestaurant]);
				
				try {
					cfp.setContentObject(new Message("reserver-restaurant","1"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				cfp.setReplyWith("cfp"+System.currentTimeMillis());
				myAgent.send(cfp);
				nombreAppel++;

				mt = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
				step = 1;
				state = 2;

				break;
			case 1:

				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						System.out.println("Persone : "+getAID().getLocalName()+" successfully reserved a place from "+reply.getSender().getLocalName() +" with "+ nombreAppel +" Call(s)");
						state = 3;
						/*addBehaviour(new WakerBehaviour(myAgent, 2 * time/3) {
							protected void handleElapsedTimeout() {
								addBehaviour(new LeaveRestaurant());
							}
						});*/
						reservedRestaurant = true;
						Hello.addSummary(getAID().getLocalName(), reply.getSender().getLocalName(), nombreAppel);
						//myAgent.doDelete();
					}
					else {
						System.out.println("Persone : "+getAID().getLocalName()+" failed to reserve from "+reply.getSender().getLocalName());
						state = 4;
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription sd = new ServiceDescription();
						sd.setType("people");
						template.addServices(sd);
						
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template); 
							System.out.print("Persone : "+getAID().getLocalName()+" demande au agents : [ ");

							people = new AID[result.length - 1];
							int index = 0;
							for (int i = 0; i < result.length; ++i) {
								if(!result[i].getName().equals(getAID()) && rand.nextInt(2) == 1) {
									people[index]= result[i].getName();
									System.out.print(people[index++].getLocalName() + ", ");
								}
							}
							System.out.println(" ] ce qu il font");
							addBehaviour(new SendMessageToPeople());
						}
						catch (FIPAException fe) {
							fe.printStackTrace();
						}
					}
					step = 2;

				}
				else {
					block();
				}

				break;
			}        
		}

		public boolean done() {
			return (step == 2 );
		}
	}
	
	private class LeaveRestaurant extends Behaviour {

		private MessageTemplate mt;
		private int step = 0;

		public void action() {
			
			switch (step) {
			case 0:

				System.out.println( "\nPerson : " + myAgent.getLocalName() + " is going to leave restaurant " + restaurants[indexRandomRestaurant].getLocalName());
				
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				cfp.addReceiver(restaurants[indexRandomRestaurant]);
				
				try {
					cfp.setContentObject(new Message("leave-restaurant","1"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				cfp.setReplyWith("cfp"+System.currentTimeMillis());
				myAgent.send(cfp);

				mt = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
				step = 1;

				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						state = 5;
						System.out.println("Persone : "+getAID().getLocalName()+" finished all tasks "+reply.getSender().getLocalName());
						//myAgent.doDelete();
					}
					else {
						System.out.println("Persone : "+getAID().getLocalName()+" failed to leave from "+reply.getSender().getLocalName());
					}
					step = 2;
				}
				else {
					block();
				}

				break;
			}        
		}

		public boolean done() {
			return (step == 2 );
		}
	}
	
	private class SendMessageToPeople extends Behaviour {

		private MessageTemplate mt;
		private int step = 0;
		private int messagesRecieved = 0;

		public void action() {
			
			switch (step) {
			case 0:
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for(int i = 0; i < people.length; i++) {
					cfp.addReceiver(people[i]);
				}
				
				try {
					cfp.setContentObject(new Message("demande-inforation","1"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				cfp.setReplyWith("cfp"+System.currentTimeMillis());
				myAgent.send(cfp);

				mt = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
				step = 1;

				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						try {
							Message messageContent = (Message) reply.getContentObject();
							if(messageContent.getType().equals("reponse-information")) {
								System.out.println("Persone : "+getAID().getLocalName()+" recieved reply from "+reply.getSender().getLocalName() + " message : '" + messageContent.getContent()+ "'");
								messagesRecieved++;
							}
						} catch (UnreadableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else {
					block();
				}

				break;
			}        
		}

		public boolean done() {
			return (messagesRecieved == people.length );
		}
	}
	
	private class RecieveMessageFromPeople extends CyclicBehaviour {
		
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				try {
					Message messageContent = (Message) msg.getContentObject();
					
					if(messageContent.getType().equals("demande-inforation")) {
						ACLMessage reply = msg.createReply();
						
						System.out.println("Persone : " + myAgent.getLocalName() + " recieved a message from : " + msg.getSender().getLocalName());
						
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContentObject(new Message("reponse-information", getStateAgent()));
						
						myAgent.send(reply);
					}
				} catch (UnreadableException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
	} 

	private String getStateAgent() {
		String str = "I'am doing nothing";
		switch(state) {
		case 0: 
			str = "I'm waiting to make a reservation";
			break;
		case 1: 
			str = "I'm triyng to make a reservation";
			break;
		case 2: 
			str = "I'm calling the restaurant " + restaurants[indexRandomRestaurant].getLocalName() + " and waiting for response";
			break;
		case 3:
			str = "I reserved a place in restaurant " + restaurants[indexRandomRestaurant].getLocalName() ;
			break;
		case 4:
			str = "I failed to reseve retaurant " + restaurants[indexRandomRestaurant].getLocalName() + " now I'm tring to see what other people are doing";
			break;
		case 5:
			str = "I freed my place at the restaurant " + restaurants[indexRandomRestaurant].getLocalName();
			break;
		}
		
		return str;
	}
}