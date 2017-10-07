package test

import (
	"flag"
	"fmt"
	"testing"

	"github.com/golang/protobuf/proto"

	"google.golang.org/grpc/metadata"

	pb "github.com/qlik-oss/server-side-extension/examples/go/basic_example/gen"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

var (
	port = flag.Int("port", 50055, "The server port")
)

var expectedCapabilities = pb.Capabilities{
	AllowScript:      false,
	PluginIdentifier: "SSE Go Plugin",
	PluginVersion:    "1.0.0",
}

var functionRequestHeader = &pb.FunctionRequestHeader{
	FunctionId: 0, //EchoString
	Version:    "1.0.0",
}

var expectedRow = pb.Row{
	Duals: []*pb.Dual{
		&pb.Dual{StrData: "Hello World!"},
	},
}
var expectedBundledRows = pb.BundledRows{
	Rows: []*pb.Row{
		&expectedRow,
	},
}

func TestGetCapablities(t *testing.T) {
	flag.Parse()

	// use the port declared in the server.
	conn, err := grpc.Dial(fmt.Sprintf("127.0.0.1:%d", *port), grpc.WithInsecure())
	if err != nil {
		t.Errorf("Failed to dial: %v", err)
	}
	defer conn.Close()

	client := pb.NewConnectorClient(conn)
	actualCapabilities, err := client.GetCapabilities(context.Background(), &pb.Empty{})
	if err != nil {
		t.Errorf("Failed to GetCapabilities: %v", err)
	}
	switch {
	case actualCapabilities.AllowScript != expectedCapabilities.AllowScript:
		t.Errorf("Unexpected AllowScript: %t", actualCapabilities.AllowScript)
	case actualCapabilities.PluginIdentifier != expectedCapabilities.PluginIdentifier:
		t.Errorf("Unexpected PluginIdentifier: %s", actualCapabilities.PluginIdentifier)
	case actualCapabilities.PluginVersion != expectedCapabilities.PluginVersion:
		t.Errorf("Unexpected PluginVersion: %s", actualCapabilities.PluginVersion)
	}
}

func TestExecuteFunction(t *testing.T) {
	flag.Parse()

	conn, err := grpc.Dial(fmt.Sprintf("127.0.0.1:%d", *port), grpc.WithInsecure())
	if err != nil {
		t.Errorf("Failed to dial: %v", err)
	}
	defer conn.Close()

	// Set up the client.
	ctx, err := createClientContext()
	if err != nil {
		t.Errorf("Failed to create client context: %v", err)
	}
	executor, err := pb.NewConnectorClient(conn).ExecuteFunction(ctx)
	if err != nil {
		t.Errorf("Failed to ExecuteFunction: %v", err)
	}

	// Send and receive.
	if err := executor.Send(&expectedBundledRows); err != nil {
		t.Errorf("Failed to Send(): %v", err)
	}
	if actualBundledRows, err := executor.Recv(); err != nil {
		t.Errorf("Failed to Recv(): %v", err)
	} else {
		for _, actualRow := range actualBundledRows.Rows {
			if len(actualRow.Duals) != len(expectedRow.Duals) {
				t.Errorf("Wrong number of duals: %v", len(actualRow.Duals))
			}
			if actualRow.Duals[0].StrData != expectedRow.Duals[0].StrData {
				t.Errorf("Wrong StrData: %v", actualRow.Duals[0].StrData)
			}
		}
	}
}

func createClientContext() (context.Context, error) {
	binHdr, err := proto.Marshal(functionRequestHeader)
	if err != nil {
		return nil, err
	}
	md := metadata.Pairs("qlik-functionrequestheader-bin", string(binHdr))
	ctx := metadata.NewOutgoingContext(context.Background(), md)

	return ctx, nil
}
