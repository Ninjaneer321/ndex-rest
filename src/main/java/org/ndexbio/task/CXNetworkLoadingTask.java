package org.ndexbio.task;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.ndexbio.common.models.dao.postgresql.NetworkDAO;
import org.ndexbio.common.persistence.CXNetworkLoader;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.ProvenanceEntity;

public class CXNetworkLoadingTask implements NdexSystemTask {
	
	private static Logger logger = Logger.getLogger(CXNetworkLoadingTask.class.getName());
	
	private UUID networkId;
	private String ownerUserName;
	private boolean isUpdate;
	
	public CXNetworkLoadingTask (UUID networkUUID, String ownerName, boolean isUpdate) {
		this.networkId = networkUUID;
		this.ownerUserName = ownerName;
		this.isUpdate = isUpdate;
	}
	
	@Override
	public void run()  {
		
	  try (NetworkDAO dao = new NetworkDAO ()) {
		try ( CXNetworkLoader loader = new CXNetworkLoader(networkId, ownerUserName, isUpdate,dao) ) {
				loader.persistCXNetwork();
		} catch ( IOException | NdexException | SQLException | SolrServerException | RuntimeException e1) {
			logger.severe("Error occurred when loading network " + networkId + ": " + e1.getMessage());
			e1.printStackTrace();
			dao.setErrorMessage(networkId, e1.getMessage());
			/*} catch (SQLException e) {
				logger.severe("Error occurred when setting error message in network " + networkId + ": " + e1.getMessage());
				e.printStackTrace();
			} */
		
		} 
	  } catch (SQLException e) {
		e.printStackTrace();
		logger.severe("Failed to create NetworkDAO object: " + e.getMessage());
	  }
	}

}

