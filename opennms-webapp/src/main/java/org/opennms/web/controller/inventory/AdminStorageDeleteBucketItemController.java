/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.controller.inventory;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opennms.core.utils.WebSecurityUtils;
import org.opennms.web.api.Authentication;
import org.opennms.web.svclayer.inventory.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


/**
 * <p>AdminStorageDeleteBucketItemController class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
public class AdminStorageDeleteBucketItemController implements Controller {
	
	private static final Logger LOG = LoggerFactory.getLogger(AdminStorageDeleteBucketItemController.class);


    InventoryService m_inventoryService;
    
    /**
     * <p>getInventoryService</p>
     *
     * @return a {@link org.opennms.web.svclayer.inventory.InventoryService} object.
     */
    public InventoryService getInventoryService() {
        return m_inventoryService;
    }

    /**
     * <p>setInventoryService</p>
     *
     * @param inventoryService a {@link org.opennms.web.svclayer.inventory.InventoryService} object.
     */
    public void setInventoryService(InventoryService inventoryService) {
        m_inventoryService = inventoryService;
    }

    /** {@inheritDoc} */
    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse arg1) throws Exception {

        String node = request.getParameter("node");
        int nodeid = WebSecurityUtils.safeParseInt(node);

        String bucket = request.getParameter("bucket");
        String filename = request.getParameter("filename");
        if (bucket != null && filename != null && request.isUserInRole(Authentication.ROLE_ADMIN)) {
            boolean done = m_inventoryService.deleteBucketItem(bucket, filename);
            if (!done){
                LOG.debug("AdminStorageDeleteBucketItemController ModelAndView onSubmit error while deleting status for: {}", bucket);
            }
     }
        Map<String, Object> model  = m_inventoryService.getBuckets(nodeid);
        return new ModelAndView("admin/storage/storageAdmin","model",model);
    }
    
    


}
