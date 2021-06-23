/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project1;

import jade.core.AID;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import net.sf.clipsrules.jni.*;

/**
 *
 * @author Ricardo Gómez Hernández
 */
public class SellerAgent extends Agent{
    String[] productName = new String[5];
    float[] productPrice = new float[5];
    String[] productCategory = new String[5];
    int[] productQuantity = new int[5];
    String suppliedProduct = "";
    private AID[] supplierAgents;
    Environment clips;
    
    // Put agent initializations here
    protected void setup(){
        // Create the catalogue
        String portCon = "jdbc:mysql://localhost:3306/"; //Connection DB
        String s = getAID().getName();//Geting the name
        String[] cut = s.split("@");//Cut the name to remove the ip
        String name = cut[0];//Name to contect with the DB
        
        // Register the product-selling service in the yellow pages
	DFAgentDescription dfd = new DFAgentDescription();
	dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType("product-selling");
	sd.setName("JADE-product-trading");
	dfd.addServices(sd);
        
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            String fullCon = portCon + name;
            Connection con = DriverManager.getConnection(fullCon,"connectionJava","ricardoConnection");
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select * from product");
            int i = 0;
            
            while(rs.next()){
                productName[i] = rs.getString(1);
                productPrice[i] = rs.getFloat(2);
                productCategory[i] = rs.getString(3);
                productQuantity[i] = rs.getInt(4);
                i++;
            }
            con.close();
        }catch (Exception e){
            System.out.println(e);
        }
        
        System.out.println("### Hallo! Seller-agent "+ getAID().getName() +" is ready to sell.");
        System.out.println("### Products ready to sell");
        // Add the behaviour serving queries from buyer agents
	addBehaviour(new OfferRequestsServer());

	// Add the behaviour serving purchase orders from buyer agents
	addBehaviour(new PurchaseOrdersServer());
    }
    
    // Put agent clean-up operations here
    protected void takeDown() {
	// Deregister from the yellow pages
	try {
            DFService.deregister(this);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
	
	System.out.println("### Seller-agent "+ getAID().getName() +" terminating.");
    }
    
    
    /**
    Inner class OfferRequestsServer.
    This is the behaviour used by SellerAgent agents to serve incoming requests 
    for offer from buyer agents.
    If the requested product is in the local catalogue the seller agent replies 
    with a PROPOSE message specifying the price. Otherwise a REFUSE message is
    sent back.
    */
    
    private class OfferRequestsServer extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null){
                // CFP Message received. Process it
		String product = msg.getContent();
		ACLMessage reply = msg.createReply();
                int pos = 0;
                
                int i = 0;
                while(i < 5){
                    if(product.equals(productName[i])){
                        pos = i;
                    }
                    i++;
                }
                
                if(pos > 0){
                    // The requested product is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(productPrice[pos]));//Send the price of the product
                } else{
                    // The requested product is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else{
                block();
            }
        }
    }// End of inner class OfferRequestsServer
    
    /**
    Inner class PurchaseOrdersServer.
    This is the behaviour used by Book-seller agents to serve incoming 
    offer acceptances (i.e. purchase orders) from buyer agents.
    The seller agent removes the purchased book from its catalogue 
    and replies with an INFORM message to notify the buyer that the
    purchase has been sucesfully completed.
    */
    
    private class PurchaseOrdersServer extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            int i = 0;
            int pos = 0;
            int newQuant = 0;
            
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String product = msg.getContent();
                ACLMessage reply = msg.createReply();
        
                while(i < 5){
                    if(product.equals(productName[i]))
                        pos = i;
                    i++;
                }
            
                newQuant = productQuantity[pos] - 1;
                productQuantity[pos] = newQuant;
            
                if (pos > 0) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("### " + product +" sold to agent "+ msg.getSender().getName());
                    System.out.println("\n### Inventory after sell the product ###");
                    System.out.println("Name: " + productName[pos]);
                    System.out.println("Priece: " + productPrice[pos]);
                    System.out.println("Category : " + productCategory[pos]);
                    System.out.println("Quantity of products = " + productQuantity[pos] + "\n");
                    
                    String nameB = msg.getSender().getName();
                    String[] cut = nameB.split("@");//Cut the name
                    tellBehaviour();
                    askBehaviour(pos, cut[0], cut[1]);
                } else {
                    // The requested product has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                
                myAgent.send(reply);
                
                if(newQuant == 0){
                    //Call the suplier 
                    System.out.println("### We need to supply " + productName[pos] + "!!!");
                    suppliedProduct = productName[pos];
                    addBehaviour(new SupplyProduct());
                }else {
                    String s = getAID().getName();//Geting the name
                    String[] cut = s.split("@");//Cut the name to remove the ip
                    String datab = cut[0];//Name to contect with the DB
                    String portCon = "jdbc:mysql://localhost:3306/";
                    
                    try{
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        String fullCon = portCon + datab;
                        Connection con = DriverManager.getConnection(fullCon,"connectionJava","ricardoConnection");
                        Statement stmt = con.createStatement();
                        String update = "update product set quantity = " + productQuantity[pos];
                        update = update + " where name = ";
                        update = update + "\"" + productName[pos] + "\"";
                        stmt.executeUpdate(update);
                        
                        System.out.println("### Database updated");
                        con.close();
                    }catch (Exception e){
                        System.out.println(e);
                    }
                }
            } else {
                block();
            }
        }
    } // End of inner class OfferRequestsServer
    
    public class SupplyProduct extends OneShotBehaviour{
        
        public void action(){
            if (suppliedProduct != ""){
            System.out.println("### Trying to supply "+ suppliedProduct);
                    
            // Update the list of seller agents
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("product-supplying");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template); 
                System.out.println("### Found the following supplier agents:");
                supplierAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    supplierAgents[i] = result[i].getName();
                    System.out.println(supplierAgents[i].getName());
                }
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }

        // Perform the request
            myAgent.addBehaviour(new RequestPerformer());
            
        } else{
            // Make the agent terminate
            System.out.println("### No target product name specified");
        }
        }
    }
    
    private class RequestPerformer extends Behaviour{
        private AID supplierA; //Supplier with the product
	private int repliesCnt = 0; // The counter of replies from supplier agents
	private MessageTemplate mt; // The template to receive replies
	private int step = 0;
        private int quantitySupplied = 0;
        
        public void action(){
            switch(step){
                case 0:
                    // Send the cfp to all suppliers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < supplierAgents.length; ++i) {
			cfp.addReceiver(supplierAgents[i]);
                    }
                    
                    cfp.setContent(suppliedProduct);
                    cfp.setConversationId("product-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("product-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from supplier agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
			if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer 
                            quantitySupplied = Integer.parseInt(reply.getContent());
                            supplierA = reply.getSender();
                            System.out.println("### Propose acepted");
                        }
                        
                        repliesCnt++;
			if (repliesCnt >= supplierAgents.length) {
                            // We received all replies
                            step = 2; 
			}
                    } else {
			block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the supplied that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(supplierA);
                    order.setContent(suppliedProduct);
                    order.setConversationId("product-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("product-trade"),
				MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    String s = getAID().getName();//Geting the name
                    String[] cut = s.split("@");//Cut the name to remove the ip
                    String datab = cut[0];//Name to contect with the DB
                    String portCon = "jdbc:mysql://localhost:3306/";
                    
                    if (reply != null) {
			// Purchase order reply received
			if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            int i = 0;
                            while(i < 5){
                                if(suppliedProduct.equals(productName[i])){
                                    productQuantity[i] = quantitySupplied;
                                    System.out.println("\n### Supplied ###");
                                    System.out.println("**New inventory**");
                                    System.out.println("Name: " + productName[i]);
                                    System.out.println("Priece: " + productPrice[i]);
                                    System.out.println("Category : " + productCategory[i]);
                                    System.out.println("Quantity of products = " + productQuantity[i] + "\n");
                                    
                                    try{
                                        Class.forName("com.mysql.cj.jdbc.Driver");
                                        String fullCon = portCon + datab;
                                        Connection con = DriverManager.getConnection(fullCon,"connectionJava","ricardoConnection");
                                        Statement stmt = con.createStatement();
                                        String update = "update product set quantity = " + productQuantity[i];
                                        update = update + " where name = ";
                                        update = update + "\"" + productName[i] + "\"";
                                        stmt.executeUpdate(update);

                                        System.out.println("### Database updated");
                                        con.close();
                                    }catch (Exception e){
                                        System.out.println(e);
                                    }
                                }
                                i++;
                            }
                            System.out.println("### " + suppliedProduct + " successfully supplied from agent " + reply.getSender().getName());
			} else {
                            System.out.println("### Attempt failed: requested product not supplied.");
			}
			step = 4;
                    } else {
			block();
                    }
                    break;
            }
        }
        
        public boolean done(){
            if (step == 2 && supplierA == null) {
		System.out.println("### Attempt failed: "+ suppliedProduct +" not available for sale");
            }
	return ((step == 2 && supplierA == null) || step == 4);
        }
    }
    
    void tellBehaviour(){
        System.out.println("### Tell is executed");
        clips = new Environment();
        clips.eval("(clear)");
        clips.load("/Users/Equipo/Desktop/Clips/clips_jni_051/loadTest.clp");
        clips.load("/Users/Equipo/Desktop/Clips/clips_jni_051/rulesTest.clp");
    }
    
    void askBehaviour(int posProd, String custNam, String custAdrs){
        String assertProd = "";
        String assertCust = "";
        String assertOrd = "";
        
        assertProd = "(product (nameProd \"" + productName[posProd] + "\" " + ") (category " + productCategory[posProd] + ") (price " + 
                productPrice[posProd] + "))";
        assertCust = "(customer (nameCust \"" + custNam + "\") (address \"" + custAdrs + "\"))";
        assertOrd = "(order (order-number " + posProd + ") (customer-name \"" + custNam + "\") (product-name \"" + 
                 productName[posProd] + "\"))";
        
        System.out.println("### Ask is executed");
        clips.assertString(assertProd);
        clips.assertString(assertCust);
        clips.assertString(assertOrd);
        clips.run();
    }
}
