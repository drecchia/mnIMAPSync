/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.marcnuri.mnimapsync.store;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.sun.mail.imap.IMAPMessage;

/**
 *
 * @author Marc Nuri <marc@marcnuri.com>
 */
public final class MessageDeleter implements Runnable {

	// **************************************************************************************************
	// Fields
	// **************************************************************************************************
	private final StoreDeleter storeDeleter;
	private final String sourceFolderName;
	private final String targetFolderName;
	private final int start;
	private final int end;
	private final boolean expunge;
	private final Set<MessageId> sourceFolderMessages;

	// **************************************************************************************************
	// Constructors
	// **************************************************************************************************
	public MessageDeleter( StoreDeleter storeDeleter, String sourceFolderName, String targetFolderName, int start, int end, boolean expunge, Set<MessageId> sourceFolderMessages ) {

		this.storeDeleter = storeDeleter;
		this.sourceFolderName = sourceFolderName;
		this.targetFolderName = targetFolderName;
		this.start = start;
		this.end = end;
		this.expunge = expunge;
		this.sourceFolderMessages = sourceFolderMessages;
	}

	// **************************************************************************************************
	// Abstract Methods
	// **************************************************************************************************
	// **************************************************************************************************
	// Overridden Methods
	// **************************************************************************************************
	public void run() {

		long deleted = 0l, skipped = 0l;
		try {
			final Folder targetFolder = storeDeleter.getTargetStore().getFolder( targetFolderName );
			// Opens a new connection per Thread
			targetFolder.open( Folder.READ_WRITE );
			final Message[] targetMessages = targetFolder.getMessages( start, end );
			targetFolder.fetch( targetMessages, MessageId.addHeaders( new FetchProfile() ) );
			for ( Message message : targetMessages ) {
				try {
					final MessageId id = new MessageId( (IMAPMessage) message );
					if ( !sourceFolderMessages.contains( id ) ) {
						message.setFlag( Flags.Flag.DELETED, true );
						deleted++;
					} else {
						skipped++;
					}
				} catch ( MessageId.MessageIdException ex ) {
					// Usually messages that ran into this exception are spammy, so we skip them.
					skipped++;
				}
			}
			// Close folder and expunge flagged messages
			// Expunge only if folder is read write
			if ( targetFolder.getMode() == Folder.READ_ONLY ) {
				targetFolder.close( false );
			} else {
				targetFolder.close( expunge );
			}
		} catch ( MessagingException messagingException ) {
			Logger.getLogger( StoreIndex.class.getName() ).log( Level.SEVERE, null, messagingException );
		}
		storeDeleter.updatedMessagesDeletedCount( deleted );
		storeDeleter.updateMessagesSkippedCount( skipped );
	}
	// **************************************************************************************************
	// Other Methods
	// **************************************************************************************************
	// **************************************************************************************************
	// Getter/Setter Methods
	// **************************************************************************************************
	// **************************************************************************************************
	// Static Methods
	// **************************************************************************************************
	// **************************************************************************************************
	// Inner Classes
	// **************************************************************************************************
}
