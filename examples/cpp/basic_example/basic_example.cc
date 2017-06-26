#define _WIN32_WINNT 0x0A00
#define MAX_BUNDLE_SIZE 64 * 1024

#include <iostream>
#include <fstream>
#include <cmath>
#include <memory>
#include <numeric>
#include <sstream>
#include <string>

#include <grpc/grpc.h>
#include <grpc++/grpc++.h>
#include <google/protobuf/stubs/common.h>

#include "ServerSideExtension.pb.h"
#include "ServerSideExtension.grpc.pb.h"

using grpc::Server;
using grpc::ServerBuilder;
using grpc::ServerContext;
using grpc::Status;

using namespace qlik::sse;


class SseServiceImpl final : public Connector::Service {

    enum available_functions {
        sum_of_rows,
        sum_of_column,
        two_norm_ranking,
        invalid
    };

    virtual ::grpc::Status GetCapabilities(::grpc::ServerContext* context, const ::qlik::sse::Empty* request, ::qlik::sse::Capabilities* response) override
    {
        std::cout << "GetCapabilities called." << std::endl;

        // SumOfRows
        auto* f = response->add_functions();
        f->set_name("SumOfRows");
        f->set_functionid(available_functions::sum_of_rows);
        f->set_functiontype(FunctionType::SCALAR);
        f->set_returntype(DataType::NUMERIC);

        auto* p = f->add_params();
        p->set_name("Col1");
        p->set_datatype(DataType::NUMERIC);

        p = f->add_params();
        p->set_name("Col2");
        p->set_datatype(DataType::NUMERIC);

        // SumOfColumn
        f = response->add_functions();
        f->set_name("SumOfColumn");
        f->set_functionid(available_functions::sum_of_column);
        f->set_functiontype(FunctionType::AGGREGATION);
        f->set_returntype(DataType::NUMERIC);

        p = f->add_params();
        p->set_name("Col1");
        p->set_datatype(DataType::NUMERIC);

        // TwoNormRanking
        f = response->add_functions();
        f->set_name("TwoNormRanking");
        f->set_functionid(available_functions::two_norm_ranking);
        f->set_functiontype(FunctionType::TENSOR);
        f->set_returntype(DataType::NUMERIC);

        p = f->add_params();
        p->set_name("Col1");
        p->set_datatype(DataType::NUMERIC);

        p = f->add_params();
        p->set_name("Col2");
        p->set_datatype(DataType::NUMERIC);

        return Status::OK;
    }

    ::grpc::Status SumOfRows(::grpc::ServerContext* context, ::grpc::ServerReaderWriter< ::qlik::sse::BundledRows, ::qlik::sse::BundledRows>* stream) {

        BundledRows rows_in, rows_out;
        while (stream->Read(&rows_in)) {
            rows_out.clear_rows();
            for (auto& row : rows_in.rows()) {
                auto* dual_out = rows_out.add_rows()->add_duals();
                double total_sum = 0;
                for (auto& dual : row.duals()) {
                    total_sum += dual.numdata();
                }
                dual_out->set_numdata(total_sum);
            }
            stream->Write(rows_out);
            rows_in.Clear();
        }

        return Status::OK;
    }

    ::grpc::Status SumOfColumn(::grpc::ServerContext* context, ::grpc::ServerReaderWriter< ::qlik::sse::BundledRows, ::qlik::sse::BundledRows>* stream) {

        BundledRows rows_in, rows_out;
        double total_sum = 0;
        while (stream->Read(&rows_in)) {
            for (auto& row : rows_in.rows()) {
                total_sum += row.duals(0).numdata();
            }
            rows_in.Clear();
        }
        auto* dual_out = rows_out.add_rows()->add_duals();
        dual_out->set_numdata(total_sum);
        stream->Write(rows_out);

        return Status::OK;
    }

    size_t get_cardinality(::grpc::ServerContext* context) {
        auto header_iterator = context->client_metadata().find("qlik-commonrequestheader-bin");

        if (header_iterator != context->client_metadata().end()) {
            CommonRequestHeader header;
            header.ParseFromString(header_iterator->second.data());
            return header.cardinality();
        }
        return 0;
    }

    ::grpc::Status TwoNormRank(::grpc::ServerContext* context, ::grpc::ServerReaderWriter< ::qlik::sse::BundledRows, ::qlik::sse::BundledRows>* stream) {

        BundledRows rows_in;
        std::vector<double> two_norm(get_cardinality(context));
        size_t i = 0;
        while (stream->Read(&rows_in)) {
            for (const auto& row : rows_in.rows()) {
                two_norm[i] = sqrt(pow(row.duals(0).numdata(), 2.0) + pow(row.duals(1).numdata(), 2.0));
                i++;
            }
        }

        std::vector<size_t> idx(two_norm.size());
        std::iota(idx.begin(), idx.end(), 0);
        sort(idx.begin(), idx.end(), [&two_norm](size_t ix, size_t jx) {return two_norm[ix] < two_norm[jx]; });

        BundledRows rows_out;
        size_t j = 0;
        while (j < idx.size()) {
            for (size_t k = j; k < idx.size() && k - j <= MAX_BUNDLE_SIZE; k++) {
                rows_out.add_rows()->add_duals()->set_numdata((double)idx[k]);
            }
            stream->Write(rows_out);
            rows_out.Clear();
            j += MAX_BUNDLE_SIZE;
        }

        return Status::OK;
    }

    int get_function_id(::grpc::ServerContext* context) {
        auto header_iterator = context->client_metadata().find("qlik-functionrequestheader-bin");

        if (header_iterator != context->client_metadata().end()) {
            FunctionRequestHeader header;
            header.ParseFromString(header_iterator->second.data());
            return header.functionid();
        }
        return available_functions::invalid;
    }

    virtual ::grpc::Status ExecuteFunction(::grpc::ServerContext* context, ::grpc::ServerReaderWriter< ::qlik::sse::BundledRows, ::qlik::sse::BundledRows>* stream) override
    {
        int funcID = get_function_id(context);

        std::cout << "ExecuteFunction called with function id: " << funcID << std::endl;

        switch (funcID)
        {
        case available_functions::sum_of_rows:
            return SumOfRows(context, stream);
        case available_functions::sum_of_column:
            return SumOfColumn(context, stream);
        case available_functions::two_norm_ranking:
            return TwoNormRank(context, stream);
        default:
            return Status::CANCELLED;
        }
    }
};

std::string read_file_to_string(std::string filename) {
    std::ifstream inFile;
    inFile.open(filename);
    if (!inFile.is_open()) {
        std::cerr << "Could not open '" << filename << "'" << std::endl;
        return "";
    }

    std::stringstream strStream;
    strStream << inFile.rdbuf();//read the file
    return strStream.str();//str holds the content of the file
}

void RunServer(const std::string& pem_dir, const std::string& insecure_port, const std::string& secure_port) {

    ServerBuilder builder;
    if (!pem_dir.empty()) // Setup secure connection with mutual authentication.
    {
        grpc::SslServerCredentialsOptions credentials_options;
        grpc::SslServerCredentialsOptions::PemKeyCertPair key_cert;

        key_cert.private_key = read_file_to_string(pem_dir + "sse_server_key.pem");
        key_cert.cert_chain = read_file_to_string(pem_dir + "sse_server_cert.pem");
        credentials_options.pem_key_cert_pairs.push_back(key_cert);

        credentials_options.pem_root_certs = read_file_to_string(pem_dir + "root_cert.pem");
        credentials_options.force_client_auth = true;

        std::shared_ptr<grpc::ServerCredentials> credentials = grpc::SslServerCredentials(credentials_options);
        std::string server_address = "0.0.0.0:" + secure_port;
        builder.AddListeningPort(server_address, credentials);
        std::cout << "SSL server listening on " << server_address << std::endl;
    }
    else // Setup insecure connection.
    {
        std::shared_ptr<grpc::ServerCredentials> credentials = grpc::InsecureServerCredentials();
        std::string server_address = "0.0.0.0:" + insecure_port;
        builder.AddListeningPort(server_address, credentials);
        std::cout << "Insecure server listening on " << server_address << std::endl;
    }

    // Register the service through which we'll communicate with clients.
    SseServiceImpl service;
    builder.RegisterService(&service);

    // Assemble the server.
    std::unique_ptr<Server> server(builder.BuildAndStart());

    // Wait for the server to shutdown.
    if (server) {
        server->Wait();
    }
}

int main(int argc, char** argv)
{
    std::cout << "Running C++ example for Qlik Server Side Extension with" << std::endl;
    std::cout << " * gRPC v" << grpc_version_string() << " and" << std::endl;
    std::cout << " * protobuf v" << google::protobuf::internal::VersionString(GOOGLE_PROTOBUF_VERSION) << std::endl;
    std::cout << "To use secure connection use the command line argument --pem_dir <dir/of/pem/and/key/>." << std::endl << std::endl;

    std::string pem_dir = "";
    if (argc == 3 && strcmp("--pem_dir", argv[2]) == 0) {
        pem_dir = argv[3];
    }

    // With little effort the port numbers could be taken in through command line args as well.
    std::string insecure_port = "50061";
    std::string secure_port = "50062";

    GOOGLE_PROTOBUF_VERIFY_VERSION;
    RunServer(pem_dir, insecure_port, secure_port);

    return 0;
}