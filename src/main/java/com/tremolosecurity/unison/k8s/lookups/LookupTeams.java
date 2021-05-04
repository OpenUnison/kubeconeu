package com.tremolosecurity.unison.k8s.lookups;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.tremolosecurity.proxy.auth.AuthInfo;
import com.tremolosecurity.proxy.filter.HttpFilterRequest;
import com.tremolosecurity.proxy.util.ProxyConstants;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scalejs.cfg.ScaleAttribute;
import com.tremolosecurity.scalejs.sdk.SourceList;
import com.tremolosecurity.server.GlobalEntries;
import com.tremolosecurity.util.NVP;
import com.tremolosecurity.provisioning.core.providers.BasicDB;
import com.tremolosecurity.proxy.auth.AuthController;

public class LookupTeams implements SourceList {

    @Override
	public void init(ScaleAttribute attribute, Map<String, Attribute> config) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<NVP> getSourceList(HttpFilterRequest request) throws Exception {
		AuthInfo userData = ((AuthController) request.getSession().getAttribute(ProxyConstants.AUTH_CTL)).getAuthInfo();
        String uid = userData.getAttribs().get("uid").getValues().get(0);

        BasicDB db = (BasicDB) GlobalEntries.getGlobalEntries().getConfigManager().getProvisioningEngine().getTarget("jitdb").getProvider();
        Connection con = db.getDS().getConnection();
        try {
            ArrayList<NVP> toReturn = new ArrayList<NVP>();
            
            PreparedStatement ps = con.prepareStatement("select localGroups.name from localUsers inner join userGroups on localUsers.userid=userGroups.userId inner join localGroups on localGroups.groupid=userGroups.groupId where sub=? and localGroups.name like 'approvers-k8s-%'");
            ps.setString(1,uid);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String name = rs.getString("name");
                name = name.substring("approvers-k8s-".length());
                
                toReturn.add(new NVP(name,name));
                
            }
            
            Collections.sort(toReturn, new Comparator<NVP>() {

                @Override
                public int compare(NVP o1, NVP o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            return toReturn;
        } finally {
            if (con != null) {
                con.close();
            }
        }
		
				
	}

	@Override
	public String validate(String value, HttpFilterRequest request) throws Exception {
		List<NVP> folders = this.getSourceList(request);
        boolean found = false;

        for (NVP nvp : folders) {
            if (nvp.getName().equalsIgnoreCase(value)) {
                found = true;
                break;
            }
        }

        if (!found) {
            return "Invalid team";
        } else {
            return null;
        }
	}
    
}
