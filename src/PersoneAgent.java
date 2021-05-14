import java.io.IOException;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
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
	Random rand = new Random();

	protected void setup() {
		System.out.println("Persone : "+getAID().getLocalName()+" is ready.");
		time = rand.nextInt(30000) + 10000;
		
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
				System.out.println("\nPersone : "+getAID().getLocalName()+" Trying to reserve a place.");

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
		} );

	}

	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("Persone : "+getAID().getLocalName()+" terminating.\n");
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