/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.integtest.send

import com.google.gson.JsonParser
import org.junit.Assert
import org.opensearch.commons.notifications.model.MethodType
import org.opensearch.commons.notifications.model.SmtpAccount
import org.opensearch.integtest.PluginRestTestCase
import org.opensearch.notifications.NotificationPlugin.Companion.PLUGIN_BASE_URI
import org.opensearch.rest.RestRequest
import org.opensearch.rest.RestStatus

internal class SendTestMessageRestHandlerIT : PluginRestTestCase() {
    @Suppress("EmptyFunctionBlock")
    fun `test send test chime message`() {
        // Create webhook notification config
        val createRequestJsonString = """
        {
            "config":{
                "name":"this is a sample config name",
                "description":"this is a sample config description",
                "config_type":"chime",
                "is_enabled":true,
                "chime":{
                    "url":"https://hooks.chime.aws/incomingwebhooks/xxxx"
                }
            }
        }
        """.trimIndent()
        val createResponse = executeRequest(
            RestRequest.Method.POST.name,
            "$PLUGIN_BASE_URI/configs",
            createRequestJsonString,
            RestStatus.OK.status
        )
        val configId = createResponse.get("config_id").asString
        Assert.assertNotNull(configId)
        Thread.sleep(1000)

        // send test message
        val sendResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/feature/test/$configId",
            "",
            RestStatus.INTERNAL_SERVER_ERROR.status
        )

        // verify failure response is with message
        val error = sendResponse.get("error").asJsonObject
        Assert.assertNotNull(error.get("reason").asString)

        // verify event is created correctly with status
        val eventId = JsonParser.parseString(error.get("reason").asString).asJsonObject.get("notification_id").asString
        val getEventResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/events/$eventId",
            "",
            RestStatus.OK.status
        )
        val items = getEventResponse.get("event_list").asJsonArray
        Assert.assertEquals(1, items.size())
        val getResponseItem = items[0].asJsonObject
        Assert.assertEquals(eventId, getResponseItem.get("event_id").asString)
        Assert.assertNotNull(getResponseItem.get("event").asJsonObject)
        Thread.sleep(100)
    }

    @Suppress("EmptyFunctionBlock")
    fun `test send test slack message`() {
        // Create webhook notification config
        val createRequestJsonString = """
        {
            "config":{
                "name":"this is a sample config name",
                "description":"this is a sample config description",
                "config_type":"slack",
                "is_enabled":true,
                "slack":{
                    "url":"https://hooks.slack.com/services/xxx/xxx"
                }
            }
        }
        """.trimIndent()
        val createResponse = executeRequest(
            RestRequest.Method.POST.name,
            "$PLUGIN_BASE_URI/configs",
            createRequestJsonString,
            RestStatus.OK.status
        )
        val configId = createResponse.get("config_id").asString
        Assert.assertNotNull(configId)
        Thread.sleep(1000)

        // send test message
        val sendResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/feature/test/$configId",
            "",
            RestStatus.INTERNAL_SERVER_ERROR.status
        )

        // verify failure response is with message
        val error = sendResponse.get("error").asJsonObject
        Assert.assertNotNull(error.get("reason").asString)

        // verify event is created correctly with status
        val eventId = JsonParser.parseString(error.get("reason").asString).asJsonObject.get("notification_id").asString

        val getEventResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/events/$eventId",
            "",
            RestStatus.OK.status
        )
        val items = getEventResponse.get("event_list").asJsonArray
        Assert.assertEquals(1, items.size())
        val getResponseItem = items[0].asJsonObject
        Assert.assertEquals(eventId, getResponseItem.get("event_id").asString)
        Assert.assertNotNull(getResponseItem.get("event").asJsonObject)
        Thread.sleep(100)
    }

    @Suppress("EmptyFunctionBlock")
    fun `test send custom webhook message`() {
        // Create webhook notification config
        val createRequestJsonString = """
        {
            "config":{
                "name":"this is a sample config name",
                "description":"this is a sample config description",
                "config_type":"webhook",
                "is_enabled":true,
                "webhook":{
                    "url":"https://szhongna.api.stdlib.com/my-webhook@dev/",
                    "header_params": {
                       "Content-type": "text/plain"
                    }
                }
            }
        }
        """.trimIndent()
        val createResponse = executeRequest(
            RestRequest.Method.POST.name,
            "$PLUGIN_BASE_URI/configs",
            createRequestJsonString,
            RestStatus.OK.status
        )
        val configId = createResponse.get("config_id").asString
        Assert.assertNotNull(configId)
        Thread.sleep(1000)

        // send test message
        val sendResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/feature/test/$configId",
            "",
            RestStatus.INTERNAL_SERVER_ERROR.status
        )

        // verify failure response is with message
        val error = sendResponse.get("error").asJsonObject
        Assert.assertNotNull(error.get("reason").asString)

        // verify event is created correctly with status
        val eventId = JsonParser.parseString(error.get("reason").asString).asJsonObject.get("notification_id").asString

        val getEventResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/events/$eventId",
            "",
            RestStatus.OK.status
        )
        val items = getEventResponse.get("event_list").asJsonArray
        Assert.assertEquals(1, items.size())
        val getResponseItem = items[0].asJsonObject
        Assert.assertEquals(eventId, getResponseItem.get("event_id").asString)
        Assert.assertNotNull(getResponseItem.get("event").asJsonObject)
        Thread.sleep(100)
    }

    @Suppress("EmptyFunctionBlock")
    fun `test send test smtp email message`() {
        val sampleSmtpAccount = SmtpAccount(
            "localhost",
            25,
            MethodType.NONE,
            "szhongna@testemail.com"
        )
        // Create smtp account notification config
        val smtpAccountCreateRequestJsonString = """
        {
            "config":{
                "name":"this is a sample smtp",
                "description":"this is a sample smtp description",
                "config_type":"smtp_account",
                "is_enabled":true,
                "smtp_account":{
                    "host":"${sampleSmtpAccount.host}",
                    "port":"${sampleSmtpAccount.port}",
                    "method":"${sampleSmtpAccount.method}",
                    "from_address":"${sampleSmtpAccount.fromAddress}"
                }
            }
        }
        """.trimIndent()
        val createResponse = executeRequest(
            RestRequest.Method.POST.name,
            "$PLUGIN_BASE_URI/configs",
            smtpAccountCreateRequestJsonString,
            RestStatus.OK.status
        )
        val smtpAccountConfigId = createResponse.get("config_id").asString
        Assert.assertNotNull(smtpAccountConfigId)
        Thread.sleep(1000)

        val emailCreateRequestJsonString = """
        {
            "config":{
                "name":"email config name",
                "description":"email description",
                "config_type":"email",
                "is_enabled":true,
                "email":{
                    "email_account_id":"$smtpAccountConfigId",
                    "recipient_list":[
                        {"recipient":"chloe@example.com"}
                    ],
                    "email_group_id_list":[]
                }
            }
        }
        """.trimIndent()

        val emailCreateResponse = executeRequest(
            RestRequest.Method.POST.name,
            "$PLUGIN_BASE_URI/configs",
            emailCreateRequestJsonString,
            RestStatus.OK.status
        )
        val emailConfigId = emailCreateResponse.get("config_id").asString
        Assert.assertNotNull(emailConfigId)
        Thread.sleep(1000)

        // send test message
        val sendResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/feature/test/$emailConfigId",
            "",
            RestStatus.SERVICE_UNAVAILABLE.status
        )

        // verify failure response is with message
        val error = sendResponse.get("error").asJsonObject
        Assert.assertNotNull(error.get("reason").asString)

        // verify event is created correctly with status
        val eventId = JsonParser.parseString(error.get("reason").asString).asJsonObject.get("notification_id").asString

        val getEventResponse = executeRequest(
            RestRequest.Method.GET.name,
            "$PLUGIN_BASE_URI/events/$eventId",
            "",
            RestStatus.OK.status
        )
        val items = getEventResponse.get("event_list").asJsonArray
        Assert.assertEquals(1, items.size())
        val getResponseItem = items[0].asJsonObject
        Assert.assertEquals(eventId, getResponseItem.get("event_id").asString)
        Assert.assertNotNull(getResponseItem.get("event").asJsonObject)
        Thread.sleep(100)
    }
}
