Run the server
--------------

Run `mvn clean package jetty:run`

TODO
----

* Switch to Activiti form properties to be compliant with latest Activiti
* Redesign current view

Overview
--------

Activiti KickStart is a webbased tool to quickly create 'adhoc' business processes using a 
subset of constructs available to the Activiti engine (http://activiti.org). 
KickStart provides a simple UI that doesn't require to learn BPMN or any modeling environment,
as it works with concepts that are familiar to every business user. 
However, the processes that are created using KickStart, are fully BPMN 2.0 compliant
and can be used as a starting point for more complex BPM endeavours.

KickStart integrates perfectly with the Activiti engine. As such, processes created with KickStart
are immediataly usable by the Activiti framework.

KickStart serves many business cases, but the following three are probably the most common:

* Simple business processes: some processes are just simple by nature, and every company has them. Think about an expense process, a holiday leave process, a hiring process, etc... These kind of processes are probably already being done using paper or e-mail. KickStart allows to model these processes quickly and change them whenever it is needed. As such, KickStart really lowers the threshold to automate these business processes.
* Prototyping: before diving into complex BPMN 2.0 modeling and thinking about the corner cases of the process, it is often wise to get all people involved aligned and work out a prototype that shows the vision of what needs to be done. KickStart allows to do exatcly that: create a business process prototype on the fly, to get your ideas visible for everyone.
* Adhoc work: in some cases, coordination is required between different people or groups in a company. You know how it normally goes: sending an email here, doing a telephone there ... which often ends up in a tarpit of nobody knowing what or when something needs to be done. However, a business process management platform such as Activiti is an excellent way of distributing and follow-up everything, as it is intended to track exactly such things. KickStart allows you to create processes for adhoc work in a matter of minutes, and distribute and coordinate tasks between people easily. 


Screenshots & Screencast
------------------------

[Screencast from Januari 2011](http://www.jorambarrez.be/blog/2011/01/05/adhoc-workflow-with-activiti-kickstart/)

Contributors
------------

* Joram Barrez (Alfresco)
* Daniel Meyer (Camunda)
* Frederik Heremans (Alfresco)
* Bernd Ruecker (Camunda)




