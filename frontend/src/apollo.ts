import { ApolloClient, InMemoryCache, HttpLink } from '@apollo/client/core'
import { setContext } from '@apollo/client/link/context'

const httpLink = new HttpLink({
  uri: import.meta.env.VITE_API_URL ?? '/graphql'
})

const authLink = setContext((_, { headers }) => {
  const token = localStorage.getItem('simpleblog_token')
  return {
    headers: {
      ...headers,
      Authorization: token ? `Bearer ${token}` : ''
    }
  }
})

export const apolloClient = new ApolloClient({
  link: authLink.concat(httpLink),
  cache: new InMemoryCache()
})
