import jade.core.Agent;

import java.io.IOException;
import java.util.Random;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class PersoneAgent extends Agent {

	private boolean status;
	private AID[] restaurants;
	private long time;
	private int indexRandomRestaurant;

	protected void setup() {
		System.out.println("Persone : "+getAID().getLocalName()+" is ready.");
		Random rand = new Random();
		time = rand.nextInt(30000) + 10000;
		
		addBehaviour(new TickerBehaviour(this, time) {
			protected void onTick() {
				System.out.println("\nPersone : "+getAID().getLocalName()+" Trying to reserve a place.");

				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("reservation-restaurant");
				template.addServices(sd);
				
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					System.out.println("Persone : "+getAID().getLocalName()+" found the following restaurant :");

					restaurants = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						restaurants[i] = result[i].getName();
						System.out.println("\t" + restaurants[i].getLocalName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
			
				myAgent.addBehaviour(new CallForReservation());

			}
		} );
	}

	protected void takeDown() {
		System.out.println("Persone : "+getAID().getLocalName()+" terminating.\n");
	}


	private class CallForReservation extends Behaviour {

		private MessageTemplate mt;
		private int step = 0;

		public void action() {
			
			switch (step) {
			case 0:

				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				Random rand = new Random(); 
				indexRandomRestaurant = rand.nextInt(restaurants.length);
				System.out.println("Personne : " + getAID().getLocalName() + " chose the restaurant : " + restaurants[indexRandomRestaurant].getLocalName());
				cfp.addReceiver(restaurants[indexRandomRestaurant]);
				
				try {
					cfp.setContentObject(new Message("reserver-restaurant","1"));
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
						System.out.println("Persone : "+getAID().getLocalName()+" successfully reserved a place from "+reply.getSender().getLocalName());
						
						addBehaviour(new WakerBehaviour(myAgent, time/2) {
							protected void handleElapsedTimeout() {
								addBehaviour(new LeaveRestaurant());
							}
						});
					}
					else {
						System.out.println("Persone : "+getAID().getLocalName()+" failed to reserve from "+reply.getSender().getLocalName());
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
						System.out.println("Persone : "+getAID().getLocalName()+" finished all tasks "+reply.getSender().getLocalName());
						myAgent.doDelete();
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

}