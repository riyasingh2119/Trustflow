"""Run with: python -m unittest lambda/test_notify.py   (boto3 is stubbed, no AWS access needed)."""
import importlib.util
import json
import os
import sys
import types
import unittest
from unittest import mock

published = []
fake_boto3 = types.ModuleType("boto3")
fake_client = mock.Mock()
fake_client.publish.side_effect = lambda **kw: published.append(kw)
fake_boto3.client = lambda name: fake_client
sys.modules["boto3"] = fake_boto3

spec = importlib.util.spec_from_file_location("notify", os.path.join(os.path.dirname(__file__), "notify.py"))
notify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notify)


def event(body, headers=None):
    return {"body": json.dumps(body), "headers": headers or {}}


class NotifyTest(unittest.TestCase):
    def setUp(self):
        published.clear()
        os.environ["TOPIC_ARN"] = "arn:aws:sns:ap-south-1:111111111111:topic"
        os.environ.pop("SHARED_SECRET", None)

    def test_publishes_alert(self):
        r = notify.handler(event({"user": "dev", "resource": "Prod DB", "score": 72, "decision": "DENY"}), None)
        self.assertEqual(r["statusCode"], 200)
        self.assertEqual(len(published), 1)
        self.assertIn("score=72", published[0]["Message"])

    def test_rejects_bad_json(self):
        r = notify.handler({"body": "{not json"}, None)
        self.assertEqual(r["statusCode"], 400)
        self.assertEqual(published, [])

    def test_shared_secret_enforced(self):
        os.environ["SHARED_SECRET"] = "s3cret"
        self.assertEqual(notify.handler(event({"user": "x"}), None)["statusCode"], 401)
        ok = notify.handler(event({"user": "x"}, {"X-TrustFlow-Key": "s3cret"}), None)
        self.assertEqual(ok["statusCode"], 200)


if __name__ == "__main__":
    unittest.main()
