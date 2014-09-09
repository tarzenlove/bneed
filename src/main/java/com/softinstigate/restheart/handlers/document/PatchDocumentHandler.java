/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.document;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.softinstigate.restheart.db.CollectionDAO;
import com.softinstigate.restheart.db.MongoDBClientSingleton;
import com.softinstigate.restheart.utils.ChannelReader;
import com.softinstigate.restheart.utils.HttpStatus;
import com.softinstigate.restheart.utils.RequestContext;
import com.softinstigate.restheart.utils.ResponseHelper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.nio.charset.Charset;
import org.bson.types.ObjectId;

/**
 *
 * @author uji
 */
public class PatchDocumentHandler implements HttpHandler
{
    final Charset charset = Charset.forName("utf-8");  

    /**
     * Creates a new instance of EntityResource
     */
    public PatchDocumentHandler()
    {
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception
    {
        RequestContext rc = new RequestContext(exchange);

        DBCollection coll = CollectionDAO.getCollection(rc.getDBName(), rc.getCollectionName());
        
        String _content = ChannelReader.read(exchange.getRequestChannel());

        DBObject content;

        try
        {
            content = (DBObject) JSON.parse(_content);
        }
        catch (JSONParseException ex)
        {
            ResponseHelper.endExchangeWithError(exchange, HttpStatus.SC_NOT_ACCEPTABLE, ex);
            return;
        }
        
        // cannot PATCH with no data
        if (content == null)
        {
            ResponseHelper.endExchange(exchange, HttpStatus.SC_NOT_ACCEPTABLE);
            return;
        }
        
        // cannot PATCH an array
        if (content instanceof BasicDBList)
        {
            ResponseHelper.endExchange(exchange, HttpStatus.SC_NOT_ACCEPTABLE);
            return;
        }

        String id = rc.getDocumentId();
        
        if (coll.count(getIdQuery(id)) != 1)
        {
            ResponseHelper.endExchange(exchange, HttpStatus.SC_NOT_FOUND);
            return;
        }
        
        content.markAsPartialObject();
        
        coll.update(getIdQuery(id), new BasicDBObject("$set", content), true, false);

        ResponseHelper.endExchange(exchange, HttpStatus.SC_OK);
    }
    
    private Object getId(String id)
    {
        try
        {
            return new ObjectId(id);
        }
        catch(IllegalArgumentException ex)
        {
            // the id is not an object id
            return id;
        }
    }
    
    private BasicDBObject getIdQuery(String id)
    {
        return new BasicDBObject("_id", getId(id));
    }
}