/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project1;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 *
 * @author Equipo
 */
public class SupplierAgent extends Agent{
    String productSupplier = "";
    
    protected void setup(){
        String s = getAID().getName();//Geting the name
        String[] cut = s.split("@");//Cut the name to remove the ip
        String name = cut[0];//Name to contect with the DB
        
        productSupplier = name;
        DFAgentDescription dfd = new DFAgentDescription();
	dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType("product-supplying");
	sd.setName("JADE-product-trade");
	dfd.addServices(sd);
        
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        System.out.println("++ Hallo! Supplier-agent "+ getAID().getName() +" is ready to supplie.");
        // Add the behaviour serving queries from buyer agents
	addBehaviour(new OfferRequestsServer());

	// Add the behaviour serving purchase orders from buyer agents
	addBehaviour(new PurchaseOrdersServer());
    }
    
    protected void takeDown() {
	// Deregister from the yellow pages
	try {
            DFService.deregister(this);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
	
	System.out.println("++ Supplier-agent "+ getAID().getName() +" terminating.");
    }
    
    private class OfferRequestsServer extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            
            if(msg != null){
                // CFP Message received. Process it
                String product = msg.getContent();
                ACLMessage reply = msg.createReply();
                
                if (product.equals(productSupplier)) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("5");
                } else {
                    // The requested product has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }else {
                block();
            }
        }
    }
    
    private class PurchaseOrdersServer extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            
            if(msg != null){
                // ACCEPT_PROPOSAL Message received. Process it
                String product = msg.getContent();
                ACLMessage reply = msg.createReply();
                
                if (product.equals(productSupplier)) {
		reply.setPerformative(ACLMessage.INFORM);
		System.out.println("++ " + product +" supplied to agent "+ msg.getSender().getName());
                } else {
                    // The requested product has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }else {
                block();
            }
        }
    }
}
