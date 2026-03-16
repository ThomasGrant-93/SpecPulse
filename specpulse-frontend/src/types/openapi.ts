// OpenAPI Specification Types

export interface OpenAPISpec {
    openapi: string;
    info: InfoObject;
    servers?: ServerObject[];
    paths: PathsObject;
    components?: ComponentsObject;
    security?: SecurityRequirementObject[];
    tags?: TagObject[];
    externalDocs?: ExternalDocumentationObject;
}

export interface InfoObject {
    title: string;
    description?: string;
    termsOfService?: string;
    contact?: ContactObject;
    license?: LicenseObject;
    version: string;
}

export interface ContactObject {
    name?: string;
    url?: string;
    email?: string;
}

export interface LicenseObject {
    name: string;
    url?: string;
}

export interface ServerObject {
    url: string;
    description?: string;
    variables?: Record<string, ServerVariableObject>;
}

export interface ServerVariableObject {
    enum?: string[];
    default: string;
    description?: string;
}

export interface PathsObject {
    [path: string]: PathItemObject;
}

export interface PathItemObject {
    summary?: string;
    description?: string;
    get?: OperationObject;
    put?: OperationObject;
    post?: OperationObject;
    delete?: OperationObject;
    options?: OperationObject;
    head?: OperationObject;
    patch?: OperationObject;
    trace?: OperationObject;
    servers?: ServerObject[];
    parameters?: ParameterObject[];
}

export interface OperationObject {
    tags?: string[];
    summary?: string;
    description?: string;
    externalDocs?: ExternalDocumentationObject;
    operationId?: string;
    parameters?: ParameterObject[];
    requestBody?: RequestBodyObject;
    responses: ResponsesObject;
    callbacks?: Record<string, CallbackObject>;
    deprecated?: boolean;
    security?: SecurityRequirementObject[];
    servers?: ServerObject[];
}

export interface ExternalDocumentationObject {
    description?: string;
    url: string;
}

export interface ParameterObject {
    name: string;
    in: 'query' | 'header' | 'path' | 'cookie';
    description?: string;
    required?: boolean;
    deprecated?: boolean;
    schema?: SchemaObject;
    example?: unknown;
    examples?: Record<string, ExampleObject>;
}

export interface RequestBodyObject {
    description?: string;
    content: MediaTypeObject;
    required?: boolean;
}

export interface MediaTypeObject {
    [mediaType: string]: MediaTypeInfo;
}

export interface MediaTypeInfo {
    schema?: SchemaObject;
    example?: unknown;
    examples?: Record<string, ExampleObject>;
}

export interface ResponsesObject {
    [statusCode: string]: ResponseObject;
}

export interface ResponseObject {
    description: string;
    headers?: Record<string, HeaderObject>;
    content?: MediaTypeObject;
    links?: Record<string, LinkObject>;
}

export interface HeaderObject {
    description?: string;
    schema?: SchemaObject;
}

export interface LinkObject {
    operationRef?: string;
    operationId?: string;
    parameters?: Record<string, unknown>;
    requestBody?: unknown;
    description?: string;
    server?: ServerObject;
}

export interface CallbackObject {
    [path: string]: PathItemObject;
}

export interface ExampleObject {
    summary?: string;
    description?: string;
    value?: unknown;
    externalValue?: string;
}

export interface SchemaObject {
    type?: string;
    format?: string;
    description?: string;
    required?: string[];
    properties?: Record<string, SchemaObject>;
    items?: SchemaObject;
    enum?: unknown[];
    default?: unknown;
    example?: unknown;
}

export interface ComponentsObject {
    schemas?: Record<string, SchemaObject>;
    parameters?: Record<string, ParameterObject>;
    responses?: Record<string, ResponseObject>;
    requestBodies?: Record<string, RequestBodyObject>;
    securitySchemes?: Record<string, SecuritySchemeObject>;
}

export interface SecuritySchemeObject {
    type: 'apiKey' | 'http' | 'oauth2' | 'openIdConnect';
    description?: string;
    name?: string;
    in?: 'query' | 'header' | 'cookie';
    scheme?: string;
    bearerFormat?: string;
}

export interface SecurityRequirementObject {
    [name: string]: string[];
}

export interface TagObject {
    name: string;
    description?: string;
    externalDocs?: ExternalDocumentationObject;
}

// Helper types for UI
export interface ApiEndpoint {
    path: string;
    method: HttpMethod;
    operationId?: string;
    summary?: string;
    description?: string;
    tags?: string[];
    parameters?: ParameterObject[];
    requestBody?: RequestBodyObject;
    responses?: ResponsesObject;
    deprecated?: boolean;
}

export type HttpMethod = 'get' | 'post' | 'put' | 'delete' | 'patch' | 'options' | 'head' | 'trace';

export interface ApiTagGroup {
    name: string;
    description?: string;
    endpoints: ApiEndpoint[];
}
