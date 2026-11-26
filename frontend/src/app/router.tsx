import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from './RootLayout'
import { LoginPage } from '../features/auth/LoginPage'
import { RegistroPage } from '../features/auth/RegistroPage'
import { RequireAuth } from '../features/auth/RequireAuth'
import { JugadorDetallePage } from '../features/jugadores/JugadorDetallePage'
import { OfertaP2PJugadorPage } from '../features/jugadores/OfertaP2PJugadorPage'
import { CarteraPage } from '../features/cartera/CarteraPage'
import { MovimientosPage } from '../features/cartera/MovimientosPage'
import { MercadoPage } from '../features/mercado/MercadoPage'
import { P2PPage } from '../features/p2p/P2PPage'
import { RankingPage } from '../features/ranking/RankingPage'
import { SubastasPage } from '../features/subastas/SubastasPage'
import { SubastaDetallePage } from '../features/subastas/SubastaDetallePage'
import { CrearSubastaPage } from '../features/subastas/CrearSubastaPage'
import { AdminRoute } from '../features/admin/AdminRoute'
import { AdminJugadoresPage } from '../features/admin/AdminJugadoresPage'
import { JugadorFormularioPage } from '../features/admin/JugadorFormularioPage'
import { RecargarSaldoPage } from '../features/admin/RecargarSaldoPage'
import { AdminMetricasPage } from '../features/admin/AdminMetricasPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <MercadoPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'registro', element: <RegistroPage /> },
      { path: 'mercado', element: <MercadoPage /> },
      { path: 'ranking', element: <RankingPage /> },
      { path: 'p2p', element: <P2PPage /> },
      { path: 'jugadores/:id', element: <JugadorDetallePage /> },
      { path: 'jugadores/:id/p2p', element: <OfertaP2PJugadorPage /> },
      { path: 'subastas', element: <SubastasPage /> },
      { path: 'subastas/:id', element: <SubastaDetallePage /> },
      {
        path: 'jugadores/:id/subasta',
        element: <RequireAuth />,
        children: [{ index: true, element: <CrearSubastaPage /> }],
      },
      {
        path: 'cartera',
        element: <RequireAuth />,
        children: [
          { index: true, element: <CarteraPage /> },
          { path: 'movimientos', element: <MovimientosPage /> },
        ],
      },
      {
        path: 'admin',
        element: <AdminRoute />,
        children: [
          { path: 'jugadores', element: <AdminJugadoresPage /> },
          { path: 'jugadores/nuevo', element: <JugadorFormularioPage /> },
          { path: 'jugadores/:id/editar', element: <JugadorFormularioPage /> },
          { path: 'saldo', element: <RecargarSaldoPage /> },
          { path: 'metricas', element: <AdminMetricasPage /> },
        ],
      },
    ],
  },
])
