package org.ndexbio.rest.services;

import java.util.Collection;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.Membership;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.rest.domain.Permissions;
import org.ndexbio.rest.helpers.IdConverter;
import com.orientechnologies.orient.core.id.ORID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNetworkService extends TestNdexService
{
    private static final NetworkService _networkService = new NetworkService(_mockRequest);
    
    
    
    
    @Test
    public void autoSuggest()
    {
        try
        {
            final ORID testNetworkRid = getRid("REACTOME TEST");
            final Collection<String> suggestions = _networkService.autoSuggestTerms(IdConverter.toJid(testNetworkRid), "RBL");
            Assert.assertNotNull(suggestions);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void autoSuggestInvalidNetwork() throws IllegalArgumentException, NdexException
    {
        _networkService.autoSuggestTerms("", "RBL");
    }

    @Test(expected = IllegalArgumentException.class)
    public void autoSuggestInvalidPartialTerm() throws IllegalArgumentException, NdexException
    {
        final ORID testNetworkRid = getRid("REACTOME TEST");
        _networkService.autoSuggestTerms(IdConverter.toJid(testNetworkRid), "");
    }

    @Test
    public void createNetwork()
    {
        Assert.assertTrue(createNewNetwork());
    }

    @Test(expected = DuplicateObjectException.class)
    public void createNetworkDuplicate() throws IllegalArgumentException, DuplicateObjectException, NdexException
    {
        Assert.assertTrue(createNewNetwork());
        
        final Network newNetwork = new Network();
        newNetwork.setCopyright("2013 Cytoscape Consortium");
        newNetwork.setDescription("This is a test network.");
        newNetwork.setFormat("JDEX");
        newNetwork.setTitle("Test Network");
        
        _networkService.createNetwork(newNetwork);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNetworkInvalid() throws IllegalArgumentException, DuplicateObjectException, NdexException
    {
        _networkService.createNetwork(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNetworkInvalidTitle() throws IllegalArgumentException, DuplicateObjectException, NdexException
    {
        final Network newNetwork = new Network();
        newNetwork.setCopyright("2013 Cytoscape Consortium");
        newNetwork.setDescription("This is a test network.");
        newNetwork.setFormat("JDEX");
        
        _networkService.createNetwork(newNetwork);
    }

    @Test
    public void deleteNetwork()
    {
        Assert.assertTrue(createNewNetwork());

        final ORID testNetworkRid = getRid("Test Network");
        Assert.assertTrue(deleteTargetNetwork(IdConverter.toJid(testNetworkRid)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNetworkInvalid() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        _networkService.deleteNetwork(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNetworkNonexistant() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        _networkService.deleteNetwork("C999R999");
    }

    @Test
    public void findNetworks()
    {
        final SearchParameters searchParameters = new SearchParameters();
        searchParameters.setSearchString("reactome");
        searchParameters.setSkip(0);
        searchParameters.setTop(25);
        
        try
        {
            _networkService.findNetworks(searchParameters);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void findNetworksInvalid() throws IllegalArgumentException, NdexException
    {
        _networkService.findNetworks(null);
    }

    @Test
    public void getNetwork()
    {
        try
        {
            final ORID networkRid = getRid("NCI_NATURE:FoxO family signaling");
            final Network testNetwork = _networkService.getNetwork(IdConverter.toJid(networkRid));
            Assert.assertNotNull(testNetwork);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNetworkInvalid() throws IllegalArgumentException, NdexException
    {
        _networkService.getNetwork("");
    }

    @Test
    public void getEdges()
    {
        try
        {
            final ORID networkRid = getRid("NCI_NATURE:FoxO family signaling");
            _networkService.getEdges(IdConverter.toJid(networkRid), 0, 25);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getEdgesInvalid() throws IllegalArgumentException, NdexException
    {
        _networkService.getEdges("", 0, 25);
    }

    @Test
    public void queryNetwork()
    {
        try
        {
            final ORID networkRid = getRid("NCI_NATURE:FoxO family signaling");
            
            final NetworkQueryParameters queryParameters = new NetworkQueryParameters();
            queryParameters.getStartingTermStrings().add("RBL_HUMAN");
            
            _networkService.queryNetwork(IdConverter.toJid(networkRid), queryParameters);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void queryNetworkInvalid() throws IllegalArgumentException, NdexException
    {
        _networkService.queryNetwork("", null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void removeMemberInvalidNetwork() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID userId = getRid("dexterpratt");

        _networkService.removeMember("", IdConverter.toJid(userId));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void removeMemberInvalidUserId() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");

        _networkService.removeMember(IdConverter.toJid(testGroupRid), "");
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void removeMemberNonexistantNetwork() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID userId = getRid("dexterpratt");

        _networkService.removeMember("C999R999", IdConverter.toJid(userId));
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void removeMemberNonexistantUser() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");

        _networkService.removeMember(IdConverter.toJid(testGroupRid), "C999R999");
    }
    
    @Test(expected = SecurityException.class)
    public void removeMemberOnlyAdminMember() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");
        final ORID userId = getRid("dexterpratt");

        _networkService.removeMember(IdConverter.toJid(testGroupRid), IdConverter.toJid(userId));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void updateMemberInvalidNetwork() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testUserId = getRid("dexterpratt");
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId(IdConverter.toJid(testUserId));
        testMembership.setResourceName("dexterpratt");

        _networkService.updateMember("", testMembership);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void updateMemberInvalidMembership() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");
        
        _networkService.updateMember(IdConverter.toJid(testGroupRid), null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void updateMemberInvalidUserId() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId("C999R999");
        testMembership.setResourceName("dexterpratt");

        _networkService.updateMember(IdConverter.toJid(testGroupRid), testMembership);
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void updateMemberNonexistantNetwork() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testUserId = getRid("dexterpratt");
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId(IdConverter.toJid(testUserId));
        testMembership.setResourceName("dexterpratt");

        _networkService.updateMember("C999R999", testMembership);
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void updateMemberNonexistantUser() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId("C999R999");
        testMembership.setResourceName("dexterpratt");

        _networkService.updateMember(IdConverter.toJid(testGroupRid), testMembership);
    }
    
    @Test(expected = SecurityException.class)
    public void updateMemberOnlyAdminMember() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");
        final ORID testUserId = getRid("dexterpratt");
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId(IdConverter.toJid(testUserId));
        testMembership.setResourceName("dexterpratt");

        _networkService.updateMember(IdConverter.toJid(testGroupRid), testMembership);
    }

    @Test
    public void updateNetwork()
    {
        try
        {
            Assert.assertTrue(createNewNetwork());

            //Refresh the user or the system won't know they have access to
            //update the network
            this.resetLoggedInUser();
            this.setLoggedInUser();

            final ORID testNetworkRid = getRid("Test Network");
            final Network testNetwork = _networkService.getNetwork(IdConverter.toJid(testNetworkRid));

            testNetwork.setTitle("Updated Test Network");
            _networkService.updateNetwork(testNetwork);
            Assert.assertEquals(_networkService.getNetwork(testNetwork.getId()).getTitle(), testNetwork.getTitle());

            Assert.assertTrue(deleteTargetNetwork(testNetwork.getId()));
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNetworkInvalid() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        _networkService.updateNetwork(null);
    }
    
    
    
    private boolean createNewNetwork()
    {
        final Network newNetwork = new Network();
        newNetwork.setCopyright("2013 Cytoscape Consortium");
        newNetwork.setDescription("This is a test network.");
        newNetwork.setFormat("JDEX");
        newNetwork.setTitle("Test Network");
        
        try
        {
            final Network createdNetwork = _networkService.createNetwork(newNetwork);
            Assert.assertNotNull(createdNetwork);
            
            return true;
        }
        catch (DuplicateObjectException doe)
        {
            return true;
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean deleteTargetNetwork(String networkId)
    {
        try
        {
            _networkService.deleteNetwork(networkId);
            Assert.assertNull(_networkService.getNetwork(networkId));
            
            return true;
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
}