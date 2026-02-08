from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.utils import new_agent_text_message
from openai import OpenAI
from dotenv import load_dotenv


# --8<-- [start:HelloWorldAgent]
class HelloWorldAgent:
    """Hello World Agent."""

    async def invoke(self, user_input) -> str:

        print( "Incoming request from remote agent: ", user_input )
        load_dotenv(override=True)

        client = OpenAI()

        response = client.responses.create(
        model="gpt-4.1",
        input=user_input )

        # print( response['output_text'] )
        # print( dir(response ) )
        # print( response.dict())

        # print( "*****\n", , "\n*******" )
        output_str = response.dict()['output'][0]['content'][0]['text']
        
        return output_str


# --8<-- [end:HelloWorldAgent]


# --8<-- [start:HelloWorldAgentExecutor_init]
class HelloWorldAgentExecutor(AgentExecutor):
    """Test AgentProxy Implementation."""

    def __init__(self):
        self.agent = HelloWorldAgent()

    # --8<-- [end:HelloWorldAgentExecutor_init]
    # --8<-- [start:HelloWorldAgentExecutor_execute]
    async def execute( self, context: RequestContext, event_queue: EventQueue,
    ) -> None:


        incoming_message_parts = context.message.parts
        user_input = incoming_message_parts[0].dict()['text']
        # print( "******\n", , "\n*******" )
                
        result = await self.agent.invoke(user_input)
        await event_queue.enqueue_event(new_agent_text_message(result))

    # --8<-- [end:HelloWorldAgentExecutor_execute]

    # --8<-- [start:HelloWorldAgentExecutor_cancel]
    async def cancel(
        self, context: RequestContext, event_queue: EventQueue
    ) -> None:
        raise Exception('cancel not supported')

    # --8<-- [end:HelloWorldAgentExecutor_cancel]
