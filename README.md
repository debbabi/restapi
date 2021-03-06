# REST API for CRISTAL [![Build Status](https://travis-ci.org/cristal-ise/restapi.svg?branch=master)](https://travis-ci.org/cristal-ise/restapi)

This is a JAX-RS wrapper for the CRISTAL Client API as specified on the [Google Doc](https://docs.google.com/document/d/1jAJyETl-iFbNXvrWa7FJLGi4mu1vEmAZNGlKlQ1I4X8/edit?usp=sharing)

The included Main may be invoked as a CRISTAL Client with -conf and -connect parameters, and will launch a Jersey http server on the URI specified in the CRISTAL property `REST.URI`, which defaults to http://localhost:8081/. Note that this property is not used outside of this example Main class.

Usage of this API requires a login cookie generated by /login. You can disable this by setting the boolean CRISTAL property `REST.requireLoginCookie` to false. 

# Settings

The REST API used several CRISTAL properties (as provided in the clc or conf files, or directly to Gateway.init as a java.util.Properties object):

 * `REST.requireLoginCookie` (boolean) - require a login cookie set at /login to access all methods. If this is false, then a login cookie may still be used, but it is not required to call any methods. If absent, job query and execution will require an 'agentName' query parameter identifying the calling agent.
 * `REST.loginCookieLife` (integer) - the number of seconds that the login cookie is valid. The cookie is set to expire at this time, but the cookie also contains a timestamp which will invalidate it after this time. 
 * `REST.allowWeakKey` (boolean) - If true, then the session key generation will fall back to a 128-bit AES key if 256-bit is not available, as with default Java installations.

 * `REST.DefaultBatchSize` (integer) - If no batch size parameter is given for Job, Event and Path queries, then this value will be used. Batch sizes for each object type may be individually set using `REST.Job.DefaultBatchSize`, `REST.Event.DefaultBatchSize` and `REST.Path.DefaultBatchSize`. If these parameters are not set, then the internal default for Paths is 75, while Job and Event are 20 each.

# Trying it out with curl

Some useful example curl commands:

* `curl -c cookies.txt http://{host:port}/login?user={user}\&pass={pass}`
  * *get a login cookie and store it in the local file cookies.txt*
* `curl -b cookies.txt http://{host:port}/domain`
  * *get root listing of the item directory*
* `curl -b cookies.txt -X OPTIONS -D - http://pcuwe04:8081/item/{uuid}/`
  * *list jobs available in the given item for the logged in user*
* `curl -b cookies.txt -H "Content-Type: text/xml" -X POST -d '<NewDevObjectDef><ObjectName>SchemaFromRest</ObjectName><SubFolder/></NewDevObjectDef>' -D - http://pcuwe04:8081/item/{uuid of SchemaFactory}/workflow/domain/CreateNewSchema?done`
  * *create a new Schema by executing the 'CreateNewSchema' activity of the CristalDev SchemaFactory item.*
* `curl -H Accept:text/xml http://{host:port}/item/{uuid}/data/{schema}/last`
  * Use `Accept:text/xml` header to retrieve XML outcome
  * Use `Accept:application/json` header to retrieve JSON outcome
    * `curl -H Accept:application/json http://{host:port}/item/{uuid}/data/{schema}/last` 
  * Use can use the Accepth header for the /item/{uuid}/history/{eventId}/data resources as well
