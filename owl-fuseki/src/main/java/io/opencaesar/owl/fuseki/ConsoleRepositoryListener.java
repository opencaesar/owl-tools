package io.opencaesar.owl.fuseki;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.log4j.Logger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;


import static java.util.Objects.requireNonNull;

/**
 * A simplistic repository listener that logs events to the console.
 */
public class ConsoleRepositoryListener
    extends AbstractRepositoryListener
{

    private final static Logger LOGGER = Logger.getLogger(ConsoleRepositoryListener.class);

    /**
     * Constructor
     */
    public ConsoleRepositoryListener() {}

    public void artifactDeployed( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Deployed " + event.getArtifact() + " to " + event.getRepository() );
    }

    public void artifactDeploying( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.debug( "Deploying " + event.getArtifact() + " to " + event.getRepository() );
    }

    public void artifactDescriptorInvalid( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.error( "Invalid artifact descriptor for " + event.getArtifact() + ": "
            + event.getException().getMessage() );
    }

    public void artifactDescriptorMissing( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.warn( "Missing artifact descriptor for " + event.getArtifact() );
    }

    public void artifactInstalled( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Installed " + event.getArtifact() + " to " + event.getFile() );
    }

    public void artifactInstalling( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.debug( "Installing " + event.getArtifact() + " to " + event.getFile() );
    }

    public void artifactResolved( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Resolved artifact " + event.getArtifact() + " from " + event.getRepository() );
    }

    public void artifactDownloading( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Downloading artifact " + event.getArtifact() + " from " + event.getRepository() );
    }

    public void artifactDownloaded( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Downloaded artifact " + event.getArtifact() + " from " + event.getRepository() );
    }

    public void artifactResolving( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.debug( "Resolving artifact " + event.getArtifact() );
    }

    public void metadataDeployed( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Deployed " + event.getMetadata() + " to " + event.getRepository() );
    }

    public void metadataDeploying( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.debug( "Deploying " + event.getMetadata() + " to " + event.getRepository() );
    }

    public void metadataInstalled( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Installed " + event.getMetadata() + " to " + event.getFile() );
    }

    public void metadataInstalling( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.debug( "Installing " + event.getMetadata() + " to " + event.getFile() );
    }

    public void metadataInvalid( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.warn( "Invalid metadata " + event.getMetadata() );
    }

    public void metadataResolved( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.info( "Resolved metadata " + event.getMetadata() + " from " + event.getRepository() );
    }

    public void metadataResolving( RepositoryEvent event )
    {
        requireNonNull( event, "event cannot be null" );
        LOGGER.debug( "Resolving metadata " + event.getMetadata() + " from " + event.getRepository() );
    }

}
