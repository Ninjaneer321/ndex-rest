package org.ndexbio.server.migration.v2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.solr.GroupIndexManager;
import org.ndexbio.common.solr.NetworkGlobalIndexManager;
import org.ndexbio.common.solr.UserIndexManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class V13DbImporter implements AutoCloseable {

	static final Logger logger = Logger.getLogger(V13DbImporter.class.getName());

	private NdexDatabase ndexDB;

	private Connection db;

	private String importFilesPrefix;

	private TypeReference<Map<String,Object>> typeRef;
	private 			ObjectMapper mapper ;

	
	public V13DbImporter(String srcFilesPath) throws NdexException, SolrServerException, IOException, SQLException {
		Configuration configuration = Configuration.createInstance();

		// create solr core for network indexes if needed.
		NetworkGlobalIndexManager mgr = new NetworkGlobalIndexManager();
		mgr.createCoreIfNotExists();
		UserIndexManager umgr = new UserIndexManager();
		umgr.createCoreIfNotExists();
		GroupIndexManager gmgr = new GroupIndexManager();
		gmgr.createCoreIfNotExists();

		// and initialize the db connections

		ndexDB = NdexDatabase.createNdexDatabase(configuration.getDBURL(), configuration.getDBUser(),
				configuration.getDBPasswd(), 10);

		this.db = ndexDB.getConnection();

		this.importFilesPrefix = srcFilesPath;
		typeRef = new TypeReference<Map<String,Object>>() {   };
		mapper = new ObjectMapper();


	}

	@Override
	public void close() throws Exception {
		db.close();
		NdexDatabase.close();

	}

	private void migrateDBAndNetworks ( ) throws FileNotFoundException, IOException, SQLException {

			importUsers();
			importGroups();
			importTasks();
			importRequests();
			importNetworks();
			
			populateSupportTables();
			
	}
	
	private void populateSupportTables() {
		String[] sqlStrs = {"insert into ndex_user (\"UUID\",creation_time,modification_time,user_name,first_name,last_name, image_url,website_url,email_addr, " +
							 "password,is_individual,description,is_deleted,is_verified) " + 
							 "select id, creation_time, modification_time, account_name, first_name,last_name,image_url,website_url,email,password,true,description,"+
							 "false,true from v1_user on conflict on constraint user_pk do nothing",
							 
							};
		
		String sqlStr1 = "insert into v1_user (rid, id, creation_time, modification_time, account_name, password,description,email,first_name,last_name,"
				+ "image_url,website_url) values (?,?,?,?,?, ?,?,?,?,?, ?,?) on conflict (id) do nothing";
		String sqlStr2 = "insert into v1_user_group (user_id,group_rid,type) values (?,?,?) on conflict on constraint v1_user_group_pkey do nothing";
		String sqlStr3 = "insert into v1_user_network (user_id, network_rid,type) values (?,?,?) on conflict on constraint v1_user_network_pkey do nothing";
		
	}
	

	private void importUsers() throws IOException, JsonProcessingException, SQLException, FileNotFoundException {
		//import users.
		try (FileInputStream i = new FileInputStream(importFilesPrefix+"/user.json")) {
			
			String sqlStr1 = "insert into v1_user (rid, id, creation_time, modification_time, account_name, password,description,email,first_name,last_name,"
					+ "image_url,website_url) values (?,?,?,?,?, ?,?,?,?,?, ?,?) on conflict (id) do nothing";
			String sqlStr2 = "insert into v1_user_group (user_id,group_rid,type) values (?,?,?) on conflict on constraint v1_user_group_pkey do nothing";
			String sqlStr3 = "insert into v1_user_network (user_id, network_rid,type) values (?,?,?) on conflict on constraint v1_user_network_pkey do nothing";

			try (PreparedStatement pstUser = db.prepareStatement(sqlStr1)) {
				try (PreparedStatement pstUserGroup = db.prepareStatement(sqlStr2)) {
					try (PreparedStatement pstUserNetwork = db.prepareStatement(sqlStr3)) {
						Iterator<Map<String,Object>> it = new ObjectMapper().readerFor(typeRef).readValues(i);
		        	
						while (it.hasNext()) {
							Map<String,Object> map = it.next();
							String accName = (String)map.get("accountName");
							logger.info("processing user " + accName );
							UUID userId = UUID.fromString((String) map.get("UUID"));
							String imageURL = (String)map.get("imageURL");
							

							// insert user rec
							pstUser.setString(1, (String)map.get("@rid"));
							pstUser.setObject(2, userId);
							pstUser.setTimestamp(3,Timestamp.valueOf((String)map.get("createdTime")));
							pstUser.setTimestamp(4,Timestamp.valueOf((String)map.get("modificationTime")));
							pstUser.setString(5, accName);
							pstUser.setString(6, (String)map.get("password"));
							pstUser.setString(7, (String)map.get("description"));
							pstUser.setString(8, (String)map.get("emailAddress"));
							pstUser.setString(9, (String)map.get("firstName"));
							pstUser.setString(10, (String)map.get("firstName"));
							if ( imageURL !=null ) {
								if ( imageURL.length() < 500 )
									pstUser.setString(11, imageURL);
								else  {
									pstUser.setString(11, null);
									logger.warning("image url length over limit, ignoring it.");
								}
							} else 
								pstUser.setString(11, null);

							pstUser.setString(12, (String)map.get("websiteURL"));
							pstUser.executeUpdate();
		    			
							// insert userGroup records
							Object o = map.get("out_groupadmin");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserGroup.setObject(1, userId);
									pstUserGroup.setString(2, (String)o);
									pstUserGroup.setString(3, "groupadmin");
									pstUserGroup.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserGroup.setObject(1, userId);
										pstUserGroup.setString(2, (String)oe);
										pstUserGroup.setString(3, "groupadmin");
										pstUserGroup.executeUpdate();
									}
								}
									
							}
							
							o = map.get("out_member");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserGroup.setObject(1, userId);
									pstUserGroup.setString(2, (String)o);
									pstUserGroup.setString(3, "member");
									pstUserGroup.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserGroup.setObject(1, userId);
										pstUserGroup.setString(2, (String)oe);
										pstUserGroup.setString(3, "member");
										pstUserGroup.executeUpdate();
									}
								}
									
							}
							
							// insert user network records
							
							o = map.get("out_admin");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, userId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "admin");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, userId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "admin");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
		    			
							o = map.get("out_admin");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, userId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "admin");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, userId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "admin");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
							
							o = map.get("out_write");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, userId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "write");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, userId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "write");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
							
							o = map.get("out_read");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, userId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "read");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, userId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "read");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
						}
					}
				}
			}
			
			db.commit();
			
		}
	}
	
	private void importGroups() throws IOException, JsonProcessingException, SQLException, FileNotFoundException {
		//import users.
		try (FileInputStream i = new FileInputStream(importFilesPrefix+"/group.json")) {
			
			String sqlStr1 = "insert into v1_group (rid, id, creation_time, modification_time, account_name, group_name,description,"
					+ "image_url,website_url) values (?,?,?,?,?, ?,?,?,?) on conflict (id) do nothing";
			String sqlStr2 = "insert into v1_group_network (group_id, network_rid,type) values (?,?,?) on conflict on constraint v1_group_network_pkey do nothing";

			try (PreparedStatement pstUser = db.prepareStatement(sqlStr1)) {
				try (PreparedStatement pstUserGroup = db.prepareStatement(sqlStr2)) {
					try (PreparedStatement pstUserNetwork = db.prepareStatement(sqlStr2)) {
						Iterator<Map<String,Object>> it = new ObjectMapper().readerFor(typeRef).readValues(i);
		        	
						while (it.hasNext()) {
							Map<String,Object> map = it.next();
							String accName = (String)map.get("accountName");
							logger.info("processing group " + accName );
							UUID groupId = UUID.fromString((String) map.get("UUID"));
							String imageURL = (String)map.get("imageURL");
							

							// insert user rec
							pstUser.setString(1, (String)map.get("@rid"));
							pstUser.setObject(2, groupId);
							pstUser.setTimestamp(3,Timestamp.valueOf((String)map.get("createdTime")));
							pstUser.setTimestamp(4,Timestamp.valueOf((String)map.get("modificationTime")));
							pstUser.setString(5, accName);
							pstUser.setString(6, (String)map.get("groupName"));
							pstUser.setString(7, (String)map.get("description"));

							if ( imageURL !=null ) {
								if ( imageURL.length() < 500 )
									pstUser.setString(8, imageURL);
								else  {
									pstUser.setString(8, null);
									logger.warning("image url length over limit, ignoring it.");
								}
							} else 
								pstUser.setString(8, null);

							pstUser.setString(9, (String)map.get("websiteURL"));
							pstUser.executeUpdate();
							
							// insert group network records
							
							Object o = map.get("out_write");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, groupId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "write");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, groupId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "write");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
							
							o = map.get("out_read");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, groupId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "read");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, groupId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "read");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
						}
					}
				}
			}
			
			db.commit();
			
		}
	}
	
	
	private void importNetworks() throws IOException, JsonProcessingException, SQLException, FileNotFoundException {
		//import users.
		try (FileInputStream i = new FileInputStream(importFilesPrefix+"/group.json")) {
			
			String sqlStr1 = "insert into v1_network (rid, id, creation_time, modification_time, account_name, group_name,description,"
					+ "image_url,website_url) values (?,?,?,?,?, ?,?,?,?) on conflict (id) do nothing";
			String sqlStr2 = "insert into v1_group_network (group_id, network_rid,type) values (?,?,?) on conflict on constraint v1_group_network_pkey do nothing";

			try (PreparedStatement pstUser = db.prepareStatement(sqlStr1)) {
				try (PreparedStatement pstUserGroup = db.prepareStatement(sqlStr2)) {
					try (PreparedStatement pstUserNetwork = db.prepareStatement(sqlStr2)) {
						Iterator<Map<String,Object>> it = new ObjectMapper().readerFor(typeRef).readValues(i);
		        	
						while (it.hasNext()) {
							Map<String,Object> map = it.next();
							String accName = (String)map.get("accountName");
							logger.info("processing group " + accName );
							UUID groupId = UUID.fromString((String) map.get("UUID"));
							String imageURL = (String)map.get("imageURL");
							

							// insert user rec
							pstUser.setString(1, (String)map.get("@rid"));
							pstUser.setObject(2, groupId);
							pstUser.setTimestamp(3,Timestamp.valueOf((String)map.get("createdTime")));
							pstUser.setTimestamp(4,Timestamp.valueOf((String)map.get("modificationTime")));
							pstUser.setString(5, accName);
							pstUser.setString(6, (String)map.get("groupName"));
							pstUser.setString(7, (String)map.get("description"));

							if ( imageURL !=null ) {
								if ( imageURL.length() < 500 )
									pstUser.setString(8, imageURL);
								else  {
									pstUser.setString(8, null);
									logger.warning("image url length over limit, ignoring it.");
								}
							} else 
								pstUser.setString(8, null);

							pstUser.setString(9, (String)map.get("websiteURL"));
							pstUser.executeUpdate();
							
							// insert group network records
							
							Object o = map.get("out_write");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, groupId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "write");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, groupId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "write");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
							
							o = map.get("out_read");
							if ( o!=null) {
								if (o instanceof String) {
									pstUserNetwork.setObject(1, groupId);
									pstUserNetwork.setString(2, (String)o);
									pstUserNetwork.setString(3, "read");
									pstUserNetwork.executeUpdate();
								} else if ( o instanceof Collection<?>) {
									for (String oe : (Collection<String>)o) {
										pstUserNetwork.setObject(1, groupId);
										pstUserNetwork.setString(2, (String)oe);
										pstUserNetwork.setString(3, "read");
										pstUserNetwork.executeUpdate();
									}
								}
									
							}
						}
					}
				}
			}
			
			db.commit();
			
		}
	}
	
	private void importTasks() throws IOException, JsonProcessingException, SQLException, FileNotFoundException {
		//import users.
		try (FileInputStream i = new FileInputStream(importFilesPrefix+"/task.json")) {
			
			String sqlStr1 = "insert into v1_task (rid, id, creation_time, modification_time,description,"
					+ "status,task_type,resource,start_time,end_time, owneruuid,format, attributes) values "
					+ "(?,?,?,?,?, ?,?,?,?,?, ?,?,? :: json) on conflict (id) do nothing";

			try (PreparedStatement pstUser = db.prepareStatement(sqlStr1)) {

						Iterator<Map<String,Object>> it = new ObjectMapper().readerFor(typeRef).readValues(i);
		        	
						while (it.hasNext()) {
							Map<String,Object> map = it.next();
							UUID taskId = UUID.fromString((String) map.get("UUID"));
							logger.info("processing task " +  taskId);


							// insert user rec
							pstUser.setString(1, (String)map.get("@rid"));
							pstUser.setObject(2, taskId);
							pstUser.setTimestamp(3,Timestamp.valueOf((String)map.get("createdTime")));
							pstUser.setTimestamp(4,Timestamp.valueOf((String)map.get("modificationTime")));
							pstUser.setString(5, (String)map.get("description"));
							pstUser.setString(6, (String)map.get("status"));
							pstUser.setString(7, (String)map.get("taskType"));

							pstUser.setObject(8, UUID.fromString((String)map.get("resource")));
						
							pstUser.setTimestamp(9, (map.get("startTime") !=null ? Timestamp.valueOf((String)map.get("startTime")): null));
							pstUser.setTimestamp(10,(map.get("endTime") != null ?  Timestamp.valueOf((String)map.get("endTime")): null));
							pstUser.setObject(11, UUID.fromString((String)map.get("ownerUUID")));
							pstUser.setString(12, (String)map.get("format"));
							pstUser.setString(13, mapper.writeValueAsString(map.get("attributes")));

							pstUser.executeUpdate();

						}
			
			}
			
			db.commit();
			
		}
	}

	private void importRequests() throws IOException, JsonProcessingException, SQLException, FileNotFoundException {
		//import users.
		try (FileInputStream i = new FileInputStream(importFilesPrefix+"/request.json")) {
			
			String sqlStr1 = "insert into v1_request (rid, id, creation_time, modification_time,source_uuid,"
					+ "destination_uuid,message,responder,response_message,response, "
					+ "in_request,out_request,request_permission,response_time) values "
					+ "(?,?,?,?,?, ?,?,?,?,?, ?,?,?,?) on conflict (id) do nothing";

			try (PreparedStatement pstUser = db.prepareStatement(sqlStr1)) {

						Iterator<Map<String,Object>> it = new ObjectMapper().readerFor(typeRef).readValues(i);
		        	
						while (it.hasNext()) {
							Map<String,Object> map = it.next();
							UUID requestId = UUID.fromString((String) map.get("UUID"));
							logger.info("processing request " +  requestId);


							// insert request rec
							pstUser.setString(1, (String)map.get("@rid"));
							pstUser.setObject(2, requestId);
							pstUser.setTimestamp(3,Timestamp.valueOf((String)map.get("createdTime")));
							pstUser.setTimestamp(4,Timestamp.valueOf((String)map.get("modificationTime")));
							pstUser.setObject(5, UUID.fromString((String)map.get("sourceUUID")));

							pstUser.setObject(6, UUID.fromString((String)map.get("destinationUUID")));
							
							pstUser.setString(7, (String)map.get("message"));
							pstUser.setString(8, (String)map.get("responder"));
							pstUser.setString(9, (String)map.get("responseMessage"));
							pstUser.setString(10, (String)map.get("response"));
							
							pstUser.setString(11, (String)map.get("in_requests"));
							pstUser.setString(12,  mapper.writeValueAsString(map.get("out_requests")));
							pstUser.setString(13,(String)map.get("requestPermission"));
							pstUser.setTimestamp(14,(map.get("responseTime") !=null ? Timestamp.valueOf((String)map.get("responseTime")) : null));
							pstUser.executeUpdate();

						}
			
			}
			
			db.commit();
			
		}
	}
	
	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.out.println(
					"Usage: V13DbImporter <Ndex 1.3 db export path>\n\n example: \n\n V13DbImporter /opt/ndex/migration\n");
			return;
		}

		try (V13DbImporter importer = new V13DbImporter(args[0])) {

			importer.migrateDBAndNetworks();
		}
		logger.info("1.3 DB migration to 2.0 completed.");
	}

}
