/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.notifications.resthandler

import org.opensearch.client.node.NodeClient
import org.opensearch.commons.notifications.NotificationConstants.DEFAULT_MAX_ITEMS
import org.opensearch.commons.notifications.NotificationConstants.EVENT_ID_LIST_TAG
import org.opensearch.commons.notifications.NotificationConstants.EVENT_ID_TAG
import org.opensearch.commons.notifications.NotificationConstants.FROM_INDEX_TAG
import org.opensearch.commons.notifications.NotificationConstants.MAX_ITEMS_TAG
import org.opensearch.commons.notifications.NotificationConstants.SORT_FIELD_TAG
import org.opensearch.commons.notifications.NotificationConstants.SORT_ORDER_TAG
import org.opensearch.commons.notifications.NotificationsPluginInterface
import org.opensearch.commons.notifications.action.GetNotificationEventRequest
import org.opensearch.commons.utils.logger
import org.opensearch.notifications.NotificationPlugin.Companion.LOG_PREFIX
import org.opensearch.notifications.NotificationPlugin.Companion.PLUGIN_BASE_URI
import org.opensearch.notifications.index.EventQueryHelper
import org.opensearch.notifications.metrics.Metrics
import org.opensearch.rest.BaseRestHandler.RestChannelConsumer
import org.opensearch.rest.BytesRestResponse
import org.opensearch.rest.RestHandler.Route
import org.opensearch.rest.RestRequest
import org.opensearch.rest.RestRequest.Method.GET
import org.opensearch.rest.RestStatus
import org.opensearch.rest.action.RestToXContentListener
import org.opensearch.search.sort.SortOrder

/**
 * Rest handler for notification events.
 */
internal class NotificationEventRestHandler : PluginBaseHandler() {
    companion object {
        private val log by logger(NotificationEventRestHandler::class.java)

        /**
         * Base URL for this handler
         */
        private const val REQUEST_URL = "$PLUGIN_BASE_URI/events"
    }

    /**
     * {@inheritDoc}
     */
    override fun getName(): String {
        return "notifications_event"
    }

    /**
     * {@inheritDoc}
     */
    override fun routes(): List<Route> {
        return listOf(
            /**
             * Get a notification event
             * Request URL: GET [REQUEST_URL/{eventId}]
             * Request body: Ref [org.opensearch.commons.notifications.action.GetNotificationEventRequest]
             * Response body: [org.opensearch.commons.notifications.action.GetNotificationEventResponse]
             */
            Route(GET, "$REQUEST_URL/{$EVENT_ID_TAG}"),
            /**
             * Get list of notification events
             * Request URL: GET [REQUEST_URL?event_id=id] or [REQUEST_URL?<query_params>]
             * <query_params> ->
             *     event_id_list=id1,id2,id3 (Other query_params ignored if this is not empty)
             *     from_index=20
             *     max_items=10
             *     sort_order=asc
             *     sort_field=event_source.severity
             *     last_updated_time_ms=from_time..to_time (Range filter field)
             *     created_time_ms=from_time..to_time (Range filter field)
             *     event_source.reference_id=abc,xyz (Keyword filter field)
             *     event_source.severity=info,high (Keyword filter field)
             *     event_source.tags=test,tags (Text filter field)
             *     event_source.title=sample title (Text filter field)
             *     status_list.config_id=abc,xyz (Keyword filter field)
             *     status_list.config_type=slack,chime (Keyword filter field)
             *     status_list.config_name=sample (Text filter field)
             *     status_list.delivery_status.status_code=400,503 (Keyword filter field)
             *     status_list.delivery_status.status_text=bad,request (Text filter field)
             *     status_list.email_recipient_status.recipient=abc,xyz (Text filter field)
             *     status_list.email_recipient_status.delivery_status.status_code=400,503 (Keyword filter field)
             *     status_list.email_recipient_status.delivery_status.status_text=bad,request (Text filter field)
             *     query=search all above keyword and text filter fields
             *     text_query=search text filter fields from above list
             * Request body: Ref [org.opensearch.commons.notifications.action.GetNotificationEventRequest]
             * Response body: [org.opensearch.commons.notifications.action.GetNotificationEventResponse]
             */
            Route(GET, REQUEST_URL)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun responseParams(): Set<String> {
        return setOf(
            EVENT_ID_TAG,
            EVENT_ID_LIST_TAG,
            SORT_FIELD_TAG,
            SORT_ORDER_TAG,
            FROM_INDEX_TAG,
            MAX_ITEMS_TAG
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun executeRequest(request: RestRequest, client: NodeClient): RestChannelConsumer {
        return when (request.method()) {
            GET -> executeGetRequest(request, client)
            else -> RestChannelConsumer {
                it.sendResponse(BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, "${request.method()} is not allowed"))
            }
        }
    }

    private fun executeGetRequest(
        request: RestRequest,
        client: NodeClient
    ): RestChannelConsumer {
        Metrics.NOTIFICATIONS_EVENTS_INFO_TOTAL.counter.increment()
        Metrics.NOTIFICATIONS_EVENTS_INFO_INTERVAL_COUNT.counter.increment()
        val eventId: String? = request.param(EVENT_ID_TAG)
        val eventIdList: String? = request.param(EVENT_ID_LIST_TAG)
        val sortField: String? = request.param(SORT_FIELD_TAG)
        val sortOrderString: String? = request.param(SORT_ORDER_TAG)
        val sortOrder: SortOrder? = if (sortOrderString == null) {
            null
        } else {
            SortOrder.fromString(sortOrderString)
        }
        val fromIndex = request.param(FROM_INDEX_TAG)?.toIntOrNull() ?: 0
        val maxItems = request.param(MAX_ITEMS_TAG)?.toIntOrNull() ?: DEFAULT_MAX_ITEMS
        val filterParams = request.params()
            .filter { EventQueryHelper.FILTER_PARAMS.contains(it.key) }
            .map { Pair(it.key, request.param(it.key)) }
            .toMap()
        log.info(
            "$LOG_PREFIX:executeGetRequest from:$fromIndex, maxItems:$maxItems," +
                " sortField:$sortField, sortOrder=$sortOrder, filters=$filterParams"
        )
        val eventRequest = GetNotificationEventRequest(
            getEventIdSet(eventId, eventIdList),
            fromIndex,
            maxItems,
            sortField,
            sortOrder,
            filterParams
        )
        return RestChannelConsumer {
            NotificationsPluginInterface.getNotificationEvent(
                client,
                eventRequest,
                RestToXContentListener(it)
            )
        }
    }

    private fun getEventIdSet(eventId: String?, eventIdList: String?): Set<String> {
        var retIds: Set<String> = setOf()
        if (eventId != null) {
            retIds = setOf(eventId)
        }
        if (eventIdList != null) {
            retIds = eventIdList.split(",").union(retIds)
        }
        return retIds
    }
}
