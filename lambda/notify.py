"""TrustFlow high-risk alert handler.

Invoked by API Gateway (HTTP API, payload format 2.0) when the backend posts a DENY with a high score.
Formats a short message, publishes it to SNS (which e-mails the administrator) and logs it to CloudWatch.
"""
import json
import os

import boto3

sns = boto3.client("sns")


def _response(code, body):
    return {"statusCode": code, "headers": {"Content-Type": "application/json"}, "body": json.dumps(body)}


def handler(event, context):
    # Optional shared secret so the endpoint is not open to anyone who finds the URL
    expected = os.environ.get("SHARED_SECRET", "")
    if expected:
        headers = {k.lower(): v for k, v in (event.get("headers") or {}).items()}
        if headers.get("x-trustflow-key") != expected:
            print("rejected: missing or wrong shared secret")
            return _response(401, {"error": "unauthorized"})

    try:
        body = json.loads(event.get("body") or "{}")
    except json.JSONDecodeError:
        return _response(400, {"error": "body must be JSON"})

    user = str(body.get("user", "unknown"))[:64]
    resource = str(body.get("resource", "unknown"))[:120]
    score = body.get("score", "?")
    decision = str(body.get("decision", "?"))[:16]
    reason = str(body.get("reason", ""))[:300]

    message = "TrustFlow high-risk event: user=%s resource=%s score=%s decision=%s\nReason: %s" % (
        user, resource, score, decision, reason)
    sns.publish(TopicArn=os.environ["TOPIC_ARN"], Subject="TrustFlow alert", Message=message)
    print(message)  # appears in CloudWatch Logs
    return _response(200, {"status": "sent"})
